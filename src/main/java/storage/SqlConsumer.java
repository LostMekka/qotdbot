package storage;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;

public interface SqlConsumer {
	
	void accept(PreparedStatement preparedStatement) throws SQLException;
}
