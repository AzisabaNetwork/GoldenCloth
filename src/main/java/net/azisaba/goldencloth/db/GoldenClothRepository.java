package net.azisaba.goldencloth.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class GoldenClothRepository {
    private final DataSource dataSource;

    public GoldenClothRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public long ensurePlayer(UUID uuid, String name) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return ensurePlayer(connection, uuid, name);
        }
    }

    public PlayerRecord findPlayerByName(String name) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return findPlayerByName(connection, name);
        }
    }

    public PlayerRecord findPlayerByUuid(UUID uuid) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return findPlayerByUuid(connection, uuid);
        }
    }

    public int getBalance(long playerId) throws SQLException {
        Instant now = Instant.now();
        try (Connection connection = dataSource.getConnection()) {
            return getBalance(connection, playerId, now);
        }
    }

    public void addPurchase(long playerId, int amount, Instant purchasedAt, Instant expiresAt, String note)
            throws SQLException {
        if (amount <= 0) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int balanceBefore = getBalance(connection, playerId, purchasedAt);
                int balanceAfter = balanceBefore + amount;
                long lotId = insertLot(connection, playerId, amount, purchasedAt, expiresAt, balanceAfter, note);
                insertTransaction(connection, playerId, lotId, amount, "purchase", note, balanceAfter);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean spend(long playerId, int amount, String reason, String reference) throws SQLException {
        if (amount <= 0) {
            return true;
        }
        Instant now = Instant.now();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int available = 0;
                long[] lotIds = new long[64];
                int[] lotRemaining = new int[64];
                int lots = 0;

                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT id, amount, spent FROM golden_cloth_lots "
                                + "WHERE player_id = ? AND expires_at > ? AND amount > spent "
                                + "ORDER BY expires_at ASC, id ASC FOR UPDATE")) {
                    select.setLong(1, playerId);
                    select.setTimestamp(2, Timestamp.from(now));
                    try (ResultSet resultSet = select.executeQuery()) {
                        while (resultSet.next()) {
                            long id = resultSet.getLong("id");
                            int amountValue = resultSet.getInt("amount");
                            int spentValue = resultSet.getInt("spent");
                            int remaining = amountValue - spentValue;
                            if (remaining <= 0) {
                                continue;
                            }
                            if (lots == lotIds.length) {
                                lotIds = growLongArray(lotIds);
                                lotRemaining = growIntArray(lotRemaining);
                            }
                            lotIds[lots] = id;
                            lotRemaining[lots] = remaining;
                            lots++;
                            available += remaining;
                        }
                    }
                }

                if (available < amount) {
                    connection.rollback();
                    return false;
                }

                int remainingToSpend = amount;
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE golden_cloth_lots SET spent = spent + ? WHERE id = ?")) {
                    for (int i = 0; i < lots && remainingToSpend > 0; i++) {
                        int take = Math.min(remainingToSpend, lotRemaining[i]);
                        update.setInt(1, take);
                        update.setLong(2, lotIds[i]);
                        update.addBatch();
                        remainingToSpend -= take;
                    }
                    update.executeBatch();
                }

                int balanceAfter = available - amount;
                insertTransaction(connection, playerId, null, -amount, reason, reference, balanceAfter);

                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public int expireLots() throws SQLException {
        Instant now = Instant.now();
        int expiredLots = 0;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT id, player_id, amount, spent FROM golden_cloth_lots "
                            + "WHERE expires_at <= ? AND amount > spent "
                            + "ORDER BY player_id ASC, expires_at ASC, id ASC FOR UPDATE")) {
                select.setTimestamp(1, Timestamp.from(now));
                try (ResultSet resultSet = select.executeQuery()) {
                    List<ExpiredLot> lots = new java.util.ArrayList<>();
                    Map<Long, Integer> totalByPlayer = new java.util.HashMap<>();

                    while (resultSet.next()) {
                        long lotId = resultSet.getLong("id");
                        long playerId = resultSet.getLong("player_id");
                        int amountValue = resultSet.getInt("amount");
                        int spentValue = resultSet.getInt("spent");
                        int remaining = amountValue - spentValue;
                        if (remaining <= 0) {
                            continue;
                        }
                        lots.add(new ExpiredLot(lotId, playerId, remaining));
                        totalByPlayer.put(playerId, totalByPlayer.getOrDefault(playerId, 0) + remaining);
                    }

                    Map<Long, Integer> balanceBeforeByPlayer = new java.util.HashMap<>();
                    Map<Long, Integer> expiredSoFarByPlayer = new java.util.HashMap<>();
                    for (Map.Entry<Long, Integer> entry : totalByPlayer.entrySet()) {
                        long playerId = entry.getKey();
                        int totalToExpire = entry.getValue();
                        int balanceAfterBaseline = getBalance(connection, playerId, now);
                        balanceBeforeByPlayer.put(playerId, balanceAfterBaseline + totalToExpire);
                        expiredSoFarByPlayer.put(playerId, 0);
                    }

                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE golden_cloth_lots SET spent = amount WHERE id = ?");
                         PreparedStatement insertTransaction = connection.prepareStatement(
                                 "INSERT INTO golden_cloth_transactions "
                                         + "(player_id, lot_id, amount_delta, reason, reference, balance_after) "
                                         + "VALUES (?, ?, ?, ?, ?, ?)")) {
                        for (ExpiredLot lot : lots) {
                            update.setLong(1, lot.lotId);
                            update.addBatch();

                            int expiredSoFar = expiredSoFarByPlayer.get(lot.playerId) + lot.remaining;
                            expiredSoFarByPlayer.put(lot.playerId, expiredSoFar);
                            int balanceAfter = balanceBeforeByPlayer.get(lot.playerId) - expiredSoFar;

                            insertTransaction.setLong(1, lot.playerId);
                            insertTransaction.setLong(2, lot.lotId);
                            insertTransaction.setInt(3, -lot.remaining);
                            insertTransaction.setString(4, "expire");
                            insertTransaction.setNull(5, java.sql.Types.VARCHAR);
                            insertTransaction.setInt(6, balanceAfter);
                            insertTransaction.addBatch();

                            expiredLots++;
                        }
                        update.executeBatch();
                        insertTransaction.executeBatch();
                    }
                }
            }
            connection.commit();
        }
        return expiredLots;
    }

    public void expireLotsSafely(Logger logger) {
        try {
            int expired = expireLots();
            if (expired > 0) {
                logger.info("Expired golden cloth lots: " + expired);
            }
        } catch (SQLException e) {
            logger.severe("Failed to expire golden cloth lots: " + e.getMessage());
        }
    }

    private long ensurePlayer(Connection connection, UUID uuid, String name) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT id, name FROM players WHERE uuid = ?")) {
            select.setString(1, uuid.toString());
            try (ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    String currentName = resultSet.getString("name");
                    if (!currentName.equals(name)) {
                        try (PreparedStatement update = connection.prepareStatement(
                                "UPDATE players SET name = ?, name_updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                            update.setString(1, name);
                            update.setLong(2, id);
                            update.executeUpdate();
                        }
                    }
                    return id;
                }
            }
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO players (uuid, name) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, uuid.toString());
            insert.setString(2, name);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        throw new SQLException("Failed to insert player row for " + uuid);
    }

    private PlayerRecord findPlayerByName(Connection connection, String name) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT id, uuid, name FROM players WHERE name = ? ORDER BY name_updated_at DESC LIMIT 1")) {
            select.setString(1, name);
            try (ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    return new PlayerRecord(
                            resultSet.getLong("id"),
                            UUID.fromString(resultSet.getString("uuid")),
                            resultSet.getString("name")
                    );
                }
            }
        }
        return null;
    }

    private PlayerRecord findPlayerByUuid(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT id, uuid, name FROM players WHERE uuid = ?")) {
            select.setString(1, uuid.toString());
            try (ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    return new PlayerRecord(
                            resultSet.getLong("id"),
                            UUID.fromString(resultSet.getString("uuid")),
                            resultSet.getString("name")
                    );
                }
            }
        }
        return null;
    }

    private int getBalance(Connection connection, long playerId, Instant now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(SUM(amount - spent), 0) AS balance "
                        + "FROM golden_cloth_lots WHERE player_id = ? AND expires_at > ?")) {
            statement.setLong(1, playerId);
            statement.setTimestamp(2, Timestamp.from(now));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("balance");
                }
            }
        }
        return 0;
    }

    private long insertLot(Connection connection, long playerId, int amount, Instant purchasedAt, Instant expiresAt,
                           int balanceAtPurchase, String note) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO golden_cloth_lots "
                        + "(player_id, purchased_at, expires_at, amount, spent, balance_at_purchase, note) "
                        + "VALUES (?, ?, ?, ?, 0, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insert.setLong(1, playerId);
            insert.setTimestamp(2, Timestamp.from(purchasedAt));
            insert.setTimestamp(3, Timestamp.from(expiresAt));
            insert.setInt(4, amount);
            insert.setInt(5, balanceAtPurchase);
            insert.setString(6, note);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to insert golden cloth lot.");
    }

    private void insertTransaction(
            Connection connection,
            long playerId,
            Long lotId,
            int amountDelta,
            String reason,
            String reference,
            int balanceAfter
    ) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO golden_cloth_transactions "
                        + "(player_id, lot_id, amount_delta, reason, reference, balance_after) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            insert.setLong(1, playerId);
            if (lotId == null) {
                insert.setNull(2, java.sql.Types.BIGINT);
            } else {
                insert.setLong(2, lotId);
            }
            insert.setInt(3, amountDelta);
            insert.setString(4, reason);
            insert.setString(5, reference);
            insert.setInt(6, balanceAfter);
            insert.executeUpdate();
        }
    }

    private long[] growLongArray(long[] input) {
        long[] next = new long[input.length * 2];
        System.arraycopy(input, 0, next, 0, input.length);
        return next;
    }

    private int[] growIntArray(int[] input) {
        int[] next = new int[input.length * 2];
        System.arraycopy(input, 0, next, 0, input.length);
        return next;
    }

    private static class ExpiredLot {
        private final long lotId;
        private final long playerId;
        private final int remaining;

        private ExpiredLot(long lotId, long playerId, int remaining) {
            this.lotId = lotId;
            this.playerId = playerId;
            this.remaining = remaining;
        }
    }

    public static class PlayerRecord {
        private final long id;
        private final UUID uuid;
        private final String name;

        public PlayerRecord(long id, UUID uuid, String name) {
            this.id = id;
            this.uuid = uuid;
            this.name = name;
        }

        public long getId() {
            return id;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }
    }
}
