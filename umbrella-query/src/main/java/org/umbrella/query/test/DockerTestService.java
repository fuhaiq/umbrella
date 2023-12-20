package org.umbrella.query.test;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umbrella.query.QueryEngine;

@Service
public class DockerTestService {
    @Autowired
    private QueryEngine engine;

//    @PostConstruct
    public void test(){
        json_engine();
        orc();
        json_duckdb();
    }

    public void json_engine() {
        var ret = engine.json("data", "file:/mnt/host/data.json",
                ctx -> ctx.resultQuery("""
        select name,detail,role from data where detail.address = 'SH' and detail.age>=40
        """).fetch());
        System.out.println(ret.format());
    }

    public void orc() {
        var ret = engine.orc("customer", "file:/mnt/host/customer.orc",
                ctx -> ctx.resultQuery("""
                        select c_custkey, c_nationkey, c_acctbal
                        from customer
                        where c_acctbal >=5000 order by c_custkey desc limit 10;
                        """).fetch());
        System.out.println(ret.format());
    }

    public void json_duckdb() {
        var ret = engine.duckdb().resultQuery("""
                select name,detail,role from '/mnt/host/data.json' where detail.address = 'SH' and detail.age>=40
                """).fetch();
        System.out.println(ret.format());
    }
}
