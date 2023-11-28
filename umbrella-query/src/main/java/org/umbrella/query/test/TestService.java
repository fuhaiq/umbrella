package org.umbrella.query.test;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.arrow.c.ArrowArrayStream;
import org.apache.arrow.c.Data;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.memory.BufferAllocator;
import org.duckdb.DuckDBConnection;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.umbrella.query.QueryEngine;
import org.umbrella.query.reader.ArrowJDBCReader;
import org.umbrella.query.reader.ArrowORCReader;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

@Component
public class TestService {
    @Autowired
    private BufferAllocator allocator;

    @Resource(name = "duckdb")
    private DSLContext duckdb;

    @Resource(name = "mysql")
    private DSLContext mysql;

    @Autowired
    private QueryEngine engine;

    @PostConstruct
    public void test2() {
        try (var rs = engine.mysql.resultQuery("""
                select * from user
                """).fetchResultSet()) {
            var ret = engine.execute("user",
                    new ArrowJDBCReader(allocator, rs),
                    ctx -> ctx.resultQuery("""
                            select * from user
                            """)).fetch();
            System.out.println(ret.format());
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }


        var ret = engine.execute("customer",
                new ArrowORCReader(allocator, "file:/Users/haiqing.fu/Downloads/parquet/customer.orc"),
                ctx -> ctx.resultQuery("""
                        select a.id,a.user_name,a.name,b.c_phone
                        from a,b
                        where a.id = b.c_custkey
                        limit 100
                        """).fetch());
        System.out.println(ret.format());
    }
    public void test() {
        var q1 = mysql.select(field("id"), field("user_name"), field("name"))
        .from(table("linkerp_staff"));
//        .where(field("id").ge(50));


//        try(var rs = q1.fetchResultSet();
//            var reader = new ArrowJDBCReader(allocator, rs)) {
//            duckdb.connection(conn -> {
//                try(conn;
//                    var arrow_array_stream = ArrowArrayStream.allocateNew(allocator)) {
//                    if(!conn.isWrapperFor(DuckDBConnection.class)) throw new IllegalStateException("驱动不对");
//                    Data.exportArrayStream(allocator, reader, arrow_array_stream);
//                    var hconn = conn.unwrap(DuckDBConnection.class);
//                    hconn.registerArrowStream("adsf", arrow_array_stream);
//
//                    var ret = DSL.using(hconn).resultQuery("select * from adsf").fetch();
//                    System.out.println(ret.format());
//                }
//            });
//
//        } catch (Throwable e) {
//            e.printStackTrace();
//            throw new RuntimeException(e);
//        }





        /*
        c_custkey integer NOT NULL,
        c_name character varying(25) NOT NULL,
        c_address character varying(40) NOT NULL,
        c_nationkey integer NOT NULL,
        c_phone character(15) NOT NULL,
        c_acctbal numeric(15,2) NOT NULL,
        c_mktsegment character(10) NOT NULL,
        c_comment character varying(117) NOT NULL
         */
        try(var rs = q1.fetchResultSet();
            var reader = new ArrowJDBCReader(allocator, rs);
            var orc = new ArrowORCReader(allocator, "file:/Users/haiqing.fu/Downloads/parquet/customer.orc")) {
            duckdb.connection(conn -> {
                try(conn;
                    var arrow_array_stream = ArrowArrayStream.allocateNew(allocator);
                    var orc_stream = ArrowArrayStream.allocateNew(allocator)) {
                    if(!conn.isWrapperFor(DuckDBConnection.class)) throw new IllegalStateException("驱动不对");
                    Data.exportArrayStream(allocator, reader, arrow_array_stream);
                    Data.exportArrayStream(allocator, orc, orc_stream);
                    var hconn = conn.unwrap(DuckDBConnection.class);
                    hconn.registerArrowStream("a", arrow_array_stream);
                    hconn.registerArrowStream("b", orc_stream);

                    var ret = DSL.using(hconn).resultQuery("""
                            select a.id,a.user_name,a.name,b.c_phone
                            from a,b
                            where a.id = b.c_custkey
                            limit 100
                            """).fetch();
                    System.out.println(ret.format());
                }
            });

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }


    }
}
