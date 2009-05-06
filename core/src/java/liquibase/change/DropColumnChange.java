package liquibase.change;

import liquibase.database.DB2Database;
import liquibase.database.Database;
import liquibase.database.SQLiteDatabase;
import liquibase.database.SQLiteDatabase.AlterTableVisitor;
import liquibase.database.structure.Index;
import liquibase.exception.InvalidChangeDefinitionException;
import liquibase.exception.JDBCException;
import liquibase.exception.UnsupportedChangeException;
import liquibase.statement.DropColumnStatement;
import liquibase.statement.ReorganizeTableStatement;
import liquibase.statement.SqlStatement;
import liquibase.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Drops an existing column from a table.
 */
public class DropColumnChange extends AbstractChange {

    private String schemaName;
    private String tableName;
    private String columnName;

    public DropColumnChange() {
        super("dropColumn", "Drop Column");
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }


    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = StringUtils.trimToNull(schemaName);
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public SqlStatement[] generateStatements(Database database) {
     
        if (database instanceof SQLiteDatabase) {		
        	// return special statements for SQLite databases
    		return generateStatementsForSQLiteDatabase(database);
		}
			
        List<SqlStatement> statements = new ArrayList<SqlStatement>();
        String schemaName = getSchemaName() == null?database.getDefaultSchemaName():getSchemaName();
        
        statements.add(new DropColumnStatement(schemaName, getTableName(), getColumnName()));
        if (database instanceof DB2Database) {
            statements.add(new ReorganizeTableStatement(schemaName, getTableName()));
        }
        
        return statements.toArray(new SqlStatement[statements.size()]);
    }
    
    private SqlStatement[] generateStatementsForSQLiteDatabase(Database database) {
    	
    	// SQLite does not support this ALTER TABLE operation until now.
		// For more information see: http://www.sqlite.org/omitted.html.
		// This is a small work around...
		
    	List<SqlStatement> statements = new ArrayList<SqlStatement>();
        
		// define alter table logic
		AlterTableVisitor rename_alter_visitor = new AlterTableVisitor() {
			public ColumnConfig[] getColumnsToAdd() {
				return new ColumnConfig[0];
			}
			public boolean createThisColumn(ColumnConfig column) {
				return !column.getName().equals(getColumnName());
			}
			public boolean copyThisColumn(ColumnConfig column) {
				return !column.getName().equals(getColumnName());
			}
			public boolean createThisIndex(Index index) {
				return !index.getColumns().contains(getColumnName());
			}
		};  
		
    	try {
    		// alter table
			statements.addAll(SQLiteDatabase.getAlterTableStatements(
					rename_alter_visitor,
					database,getSchemaName(),getTableName()));
			
		}  catch (Exception e) {
			e.printStackTrace();
		}
		
		return statements.toArray(new SqlStatement[statements.size()]);
    }

    public String getConfirmationMessage() {
        return "Column " + getTableName() + "." + getColumnName() + " dropped";
    }
}
