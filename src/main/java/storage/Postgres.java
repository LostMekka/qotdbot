package storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import xyz.elspeth.Constants;

import java.sql.*;

import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Postgres {
	
	private static final Logger logger = LoggerFactory.getLogger(Postgres.class);
	
	private final HikariDataSource dataSource;
	
	public Postgres() {
		
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(Constants.postgresUrl);
		config.setUsername(Constants.postgresUsername);
		config.setPassword(Constants.postgresPassword);
		config.setMaximumPoolSize(5);
		
		this.dataSource = new HikariDataSource(config);
		
	}
	
	public Connection getConnection() throws SQLException {
		
		return this.dataSource.getConnection();
	}
	
	public int insert(@Language("SQL") String query) {
		
		return this.insert(
			query, (ignored) -> {
			}
		);
	}
	
	public int insert(@Language("SQL") String query, SqlConsumer consumer) {
		
		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			consumer.accept(statement);
			int affectedRows = statement.executeUpdate();
			
			if (affectedRows == 0) {
				throw new SQLException("Insert failed");
			}
			
			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				var result = generatedKeys.next();
				if (result) {
					return generatedKeys.getInt(1);
				}
			}
			
			throw new SQLException("Couldn't get generated key");
			
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
			return -1;
		}
	}
	
	public boolean execute(@Language("SQL") String query) {
		
		return this.execute(
			query, (ignored) -> {
			}
		);
	}
	
	public boolean execute(@Language("SQL") String query, SqlConsumer consumer) {
		
		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement statement = connection.prepareStatement(query);
			consumer.accept(statement);
			statement.execute();
			return true;
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
			return false;
		}
	}
	
}
