package dev.joao.ledger.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

import javax.sql.DataSource;

import dev.joao.ledger.application.PersistenceException;
import dev.joao.ledger.application.Repositories;
import dev.joao.ledger.application.TransactionRunner;

public final class JdbcTransactionRunner implements TransactionRunner{
    private final DataSource dataSource;    

    public JdbcTransactionRunner(DataSource dataSource){
        this.dataSource = dataSource;
    }
    
    @Override 
    public <T> T run(Function<Repositories,T>work){
        try(Connection connection = dataSource.getConnection()){
            connection.setAutoCommit(false);
            try{
                T result = work.apply(new JdbcRepositories(connection));
                connection.commit();
                return result;
            }catch (Exception | Error e){
                connection.rollback();
                throw e;
            }
        }catch (SQLException e){
            throw new PersistenceException("Database error", e);
        }
    }
}
