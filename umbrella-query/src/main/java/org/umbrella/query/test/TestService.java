package org.umbrella.query.test;

import jakarta.annotation.PostConstruct;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.umbrella.query.QueryEngine;

@Component
public class TestService {

    @Autowired
    private QueryEngine engine;

    @PostConstruct
    /*
    `o_orderkey` bigint,
    `o_custkey` bigint,
    `o_orderstatus` string,
    `o_totalprice` double,
    `o_orderdate` string,
    `o_orderpriority` string,
    `o_clerk` string,
    `o_shippriority` int,
    `o_comment` string
     */
    public void parquet() {
        var ret = engine.duckdb.resultQuery("""
                select o_orderkey,o_orderdate,o_clerk from '/Users/haiqing.fu/Downloads/parquet/result.orders.parquet' as orders
                where orders.o_totalprice < 5000
                order by orders.o_totalprice desc limit 100
                """).fetch();
        System.out.println(ret.format());
    }


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
    public void orc() {
        var customer = engine.with("customer").orc("file:/Users/haiqing.fu/Downloads/parquet/customer.orc",
                ctx -> ctx.resultQuery("""
                        select c_custkey, c_nationkey, c_acctbal
                        from customer
                        where c_acctbal >=5000 order by c_acctbal desc limit 100;
                        """).fetch());

        System.out.println(customer.format());
    }


    public void jdbc() {
        var rs = engine.mysql.
                select().from(DSL.table("linkerp_staff")).where("phone is not null and qq is not null and address is not null");
        var ret = engine.with("staff").jdbc(rs,
                ctx -> ctx.resultQuery("""
                        select user_name,name,phone,qq,address
                        from staff
                        """).fetch()
        );
        System.out.println(ret.format());
    }
}
