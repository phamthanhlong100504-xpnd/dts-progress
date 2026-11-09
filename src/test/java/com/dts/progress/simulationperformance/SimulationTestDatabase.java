package com.dts.progress.simulationperformance;

import java.nio.file.*;
import java.sql.*;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/** Strictly owned test DB only. A business database URL is refused, even when supplied. */
final class SimulationTestDatabase implements AutoCloseable {
    final DataSource source;final TransactionTemplate transaction;private GenericContainer<?> container;
    SimulationTestDatabase() throws Exception {
        String url=System.getenv("DTS_SIMULATION_INTEGRATION_TEST_JDBC");String user="test",password="test";
        if(url!=null){
            if(!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]{1,5}/dts_integration_test_[a-z0-9_]+"))throw new IllegalArgumentException("Only an isolated test database is permitted");
            user=System.getenv().getOrDefault("DTS_SIMULATION_INTEGRATION_TEST_USER","dts_test");password=System.getenv().getOrDefault("DTS_SIMULATION_INTEGRATION_TEST_PASSWORD","");
            String child="dts_integration_test_"+java.util.UUID.randomUUID().toString().replace("-","");
            try(var c=DriverManager.getConnection(url,user,password)){
                if(!c.getCatalog().startsWith("dts_integration_test_"))throw new IllegalStateException("Not a test database");
                try(var statement=c.createStatement()){statement.execute("CREATE DATABASE "+child);}
            }
            url=url.substring(0,url.lastIndexOf('/')+1)+child;
        }else{
            container=new GenericContainer<>("postgres:16-alpine").withEnv("POSTGRES_DB","dts_integration_test_simulation").withEnv("POSTGRES_USER",user).withEnv("POSTGRES_PASSWORD",password)
                    .withExposedPorts(5432).waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\n",2));
            container.start();url="jdbc:postgresql://"+container.getHost()+":"+container.getMappedPort(5432)+"/dts_integration_test_simulation";
        }
        source=new DriverManagerDataSource(url,user,password);transaction=new TransactionTemplate(new DataSourceTransactionManager(source));
        try(var c=source.getConnection()){
            if(!c.getCatalog().startsWith("dts_integration_test_"))throw new IllegalStateException("Refusing a non-test database");
            c.setAutoCommit(false);
            for(Path path:MIGRATIONS){try(var s=c.createStatement()){s.execute(Files.readString(path));}}
            c.commit();
        }
    }
    private static final java.util.List<Path> MIGRATIONS=java.util.List.of(Path.of("src/main/resources/db/migration/V1__init_progress_schema.sql"),Path.of("src/main/resources/db/migration/V2__unique_exam_progress.sql"),Path.of("src/main/resources/db/migration/V3__question_learning_projection.sql"),Path.of("src/main/resources/db/migration/V4__simulation_performance.sql"));
    public void close(){if(container!=null)container.stop();}
}
