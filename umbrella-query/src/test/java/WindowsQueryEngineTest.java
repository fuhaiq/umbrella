import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.umbrella.query.QueryApplication;
import org.umbrella.query.QueryEngine;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = QueryApplication.class)
@FixMethodOrder
public class WindowsQueryEngineTest {

    @Autowired
    private QueryEngine engine;


    /* result.orders.parquet
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



    /* customer.orc
    c_custkey integer NOT NULL,
    c_name character varying(25) NOT NULL,
    c_address character varying(40) NOT NULL,
    c_nationkey integer NOT NULL,
    c_phone character(15) NOT NULL,
    c_acctbal numeric(15,2) NOT NULL,
    c_mktsegment character(10) NOT NULL,
    c_comment character varying(117) NOT NULL
    */

    @Test
    public void read_1() {
        var ret = engine.reader().dremio().resultQuery("""
                SELECT * FROM mongo.linkerp.staff
                """).fetch();
        System.out.println(ret.format());
    }

    @Test
    public void cache_1() {
        engine.cache("staff_orders").dremio("""
                SELECT id,name,age,role,detail,orders.o_totalprice FROM mongo.linkerp.staff,"@admin".orders
                WHERE staff.id = orders.o_custkey
                """);

        engine.cache("orders").dremio("""
                SELECT * FROM "@admin".orders limit 100;
                """);

        engine.cache("staff").dremio("""
                SELECT * FROM mysql.linkerp."linkerp_staff"
                """);
    }

    @Test
    public void cache_2() {
        var ret = engine.reader().duckdb().resultQuery("""
                select * from cache.staff_orders where detail.salary >= 30000
                """).fetch();
        System.out.println(ret.format());

        ret = engine.reader().duckdb().resultQuery("""
                select * from cache.staff limit 5
                """).fetch();
        System.out.println(ret.format());

        ret = engine.reader().duckdb().resultQuery("""
                select * from cache.orders limit 5
                """).fetch();
        System.out.println(ret.format());
    }

    @Test
    public void cache_3(){
        engine.cache("staff_orders").evict();
    }

    @Test
    public void session_1() {
        var ret = engine.session(session -> {
            session.define("T2").dremio("""
                    SELECT * FROM mongo.linkerp.staff
                    """);
            session.define("T3").dremio("""
                    SELECT * FROM mysql.linkerp."linkerp_supplier"
                    """);
            session.define("T4").dremio("""
                    SELECT * FROM mysql.linkerp."linkerp_supplier"
                    """);
            session.define("T5").dremio("""
                    SELECT * FROM mysql.linkerp."linkerp_supplier"
                    """);
            return session.dsl().resultQuery("""
                    select T2.detail.salary,T2.role[3] AS R3 from T2
                    """).fetch();
        });
        System.out.println(ret.format());
    }
}
