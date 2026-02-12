package com.github.cinnaio.milestone.storage

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.github.cinnaio.milestone.core.Progress
import org.bukkit.configuration.ConfigurationSection
import java.sql.Connection
import java.sql.Timestamp
import java.util.UUID
import java.util.logging.Logger

class MySqlRepository(private val config: ConfigurationSection, private val logger: Logger) : ProgressRepository {

    private var dataSource: HikariDataSource? = null

    override fun init() {
        val hikariConfig = HikariConfig()
        
        val host = config.getString("host", "localhost")
        val port = config.getInt("port", 3306)
        val database = config.getString("database", "milestone")
        val username = config.getString("username", "root")
        val password = config.getString("password", "")
        val ssl = config.getBoolean("ssl", false)
        
        hikariConfig.jdbcUrl = "jdbc:mysql://$host:$port/$database?useSSL=$ssl&autoReconnect=true"
        hikariConfig.driverClassName = "com.mysql.cj.jdbc.Driver"
        hikariConfig.username = username
        hikariConfig.password = password
        hikariConfig.maximumPoolSize = 10
        hikariConfig.poolName = "MilestonePool"
        
        // Recommended MySQL properties
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        
        dataSource = HikariDataSource(hikariConfig)
        
        createTable()
    }

    private fun createTable() {
        dataSource?.connection?.use { conn ->
            val stmt = conn.createStatement()
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS milestone_progress (
                    player_id VARCHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    milestone_id VARCHAR(255) NOT NULL,
                    current_count INT DEFAULT 0,
                    is_completed BOOLEAN DEFAULT 0,
                    completed_time TIMESTAMP NULL DEFAULT NULL,
                    PRIMARY KEY (player_id, milestone_id)
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """.trimIndent())
        }
    }

    override fun shutdown() {
        dataSource?.close()
    }

    override fun loadPlayerProgress(playerId: UUID): Map<String, Progress> {
        val results = mutableMapOf<String, Progress>()
        
        dataSource?.connection?.use { conn ->
            val stmt = conn.prepareStatement("SELECT * FROM milestone_progress WHERE player_id = ?")
            stmt.setString(1, playerId.toString())
            val rs = stmt.executeQuery()
            
            while (rs.next()) {
                val milestoneId = rs.getString("milestone_id")
                val progress = Progress(
                    playerId = playerId,
                    playerName = rs.getString("player_name"),
                    milestoneId = milestoneId,
                    currentCount = rs.getInt("current_count"),
                    isCompleted = rs.getBoolean("is_completed"),
                    completedTime = rs.getTimestamp("completed_time")?.toInstant()
                )
                results[milestoneId] = progress
            }
        }
        return results
    }

    override fun savePlayerProgress(progress: Progress) {
        dataSource?.connection?.use { conn ->
            saveProgressInternal(conn, progress)
        }
    }

    override fun saveAll(progressList: List<Progress>) {
        dataSource?.connection?.use { conn ->
            conn.autoCommit = false
            try {
                for (progress in progressList) {
                    saveProgressInternal(conn, progress)
                }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }

    private fun saveProgressInternal(conn: Connection, progress: Progress) {
        val stmt = conn.prepareStatement("""
            INSERT INTO milestone_progress 
            (player_id, player_name, milestone_id, current_count, is_completed, completed_time)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            player_name = VALUES(player_name),
            current_count = VALUES(current_count),
            is_completed = VALUES(is_completed),
            completed_time = VALUES(completed_time)
        """.trimIndent())
        
        stmt.setString(1, progress.playerId.toString())
        stmt.setString(2, progress.playerName)
        stmt.setString(3, progress.milestoneId)
        stmt.setInt(4, progress.currentCount)
        stmt.setBoolean(5, progress.isCompleted)
        stmt.setTimestamp(6, progress.completedTime?.let { Timestamp.from(it) })
        
        stmt.executeUpdate()
    }
}