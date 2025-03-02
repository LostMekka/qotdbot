package storage;

import discord4j.common.util.Snowflake;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
	
	public static final Database INSTANCE = new Database();
	private final       Postgres postgres = new Postgres();
	public              Logger   logger   = LoggerFactory.getLogger(Database.class);
	
	private Database() {
		
		setup();
	}
	
	private void exitInSetup() {
		
		logger.error("Failed to execute everything in setup. Exiting.");
		System.exit(1);
	}
	
	public void setup() {
		
		if (!postgres.execute("CREATE TABLE IF NOT EXISTS questions (id SERIAL PRIMARY KEY, author int8, question varchar(255), answered boolean default false)")) {
			exitInSetup();
		}
		
		logger.info("Created questions table.");
		
		if (!postgres.execute("CREATE TABLE IF NOT EXISTS suggestions (id SERIAL PRIMARY KEY, author int8, question varchar(255))")) {
			exitInSetup();
		}
		
		logger.info("Created suggestions table.");
		
	}
	
	public int addSuggestion(Snowflake author, String question) {
		
		return postgres.insert(
			"INSERT INTO public.suggestions (author, question) VALUES (?, ?)",
			(statement) -> {
				statement.setLong(1, author.asLong());
				statement.setString(2, question);
			}
		);
	}
	
	public int moveSuggestionToQuestion(int suggestionId) {
		
		try (var connection = postgres.getConnection()) {
			
			var querySuggestion = connection.prepareStatement("SELECT * FROM public.suggestions WHERE id = ?");
			querySuggestion.setInt(1, suggestionId);
			
			long   author;
			String question;
			
			try (var resultSet = querySuggestion.executeQuery()) {
				if (!resultSet.next()) {
					logger.error("Attempted reading non-existant suggestions id {}.", suggestionId);
					return -1;
				}
				
				author = resultSet.getLong("author");
				question = resultSet.getString("question");
			}
			
			connection.setAutoCommit(false);
			
			var insertStatement = connection.prepareStatement("INSERT INTO public.questions (author, question) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
			
			insertStatement.setLong(1, author);
			insertStatement.setString(2, question);
			
			insertStatement.executeUpdate();
			
			var deleteStatement = connection.prepareStatement("DELETE FROM public.suggestions WHERE id = ?");
			
			deleteStatement.setInt(1, suggestionId);
			
			deleteStatement.executeUpdate();
			
			connection.commit();
			connection.setAutoCommit(true);
			
			try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
				var result = generatedKeys.next();
				if (result) {
					return generatedKeys.getInt(1);
				}
			}
			
			logger.error("Unexpectedly got no question id from result set.");
			return -1;
		} catch (SQLException e) {
			logger.error("Failed to approve suggestion id {}.", suggestionId, e);
			return -1;
		}
	}
	
	public boolean deleteSuggestion(int suggestionId) {
		
		return postgres.execute(
			"DELETE FROM public.suggestions WHERE id = ?",
			(statement) -> {
				statement.setInt(1, suggestionId);
			}
		);
	}
	
	public Question getRandomQuestion() {
		
		try (var connection = postgres.getConnection()) {
			
			var       statement = connection.prepareStatement("SELECT * FROM public.questions WHERE answered = false ORDER BY random() LIMIT 1");
			ResultSet result    = statement.executeQuery();
			
			if (!result.next()) {
				// Currently no question
				return null;
			}
			
			return new Question(
				result.getInt("id"),
				result.getLong("author"),
				result.getString("question")
			);
			
		} catch (SQLException e) {
			return null;
		}
	}
	
	public boolean setQuestionAnswered(int questionId) {
		
		return postgres.execute(
			"UPDATE public.questions SET answered = true WHERE id = ?",
			(statement) -> {
				statement.setInt(1, questionId);
			}
		);
	}
	
	public int getQueueLength() {
		
		try (var connection = postgres.getConnection()) {
			var       statement = connection.prepareStatement("SELECT count(*) FROM public.questions WHERE answered = false");
			ResultSet result    = statement.executeQuery();
			
			if (!result.next()) {
				// Currently no question
				return -1;
			}
			
			return result.getInt(1);
		} catch (SQLException e) {
			return -1;
		}
	}
}
