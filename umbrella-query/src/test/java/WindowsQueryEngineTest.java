import jakarta.annotation.Resource;
import org.jooq.DSLContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.umbrella.query.QueryApplication;
import org.umbrella.query.QueryEngine;

import java.sql.SQLException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = QueryApplication.class)
public class WindowsQueryEngineTest {

    @Autowired
    private QueryEngine engine;

    @Resource(name = "mysql")
    private DSLContext mysql;

    @Resource(name = "dremio")
    private DSLContext dremio;


    @Test
    public void json_duckdb() {
        var ret = engine.duckdb().resultQuery("""
                select name,detail,role from 'D:/WORK/data.json' where detail.address = 'SH' and detail.age>=33
                """).fetch();
        System.out.println(ret.format());
    }

    @Test
    public void mysql() {
        var ret = engine.session(session -> {

            var rq_mysql = mysql.resultQuery("""
                    select * from test
                    """);
            session.jdbc("rq_mysql", rq_mysql);

            var rq_duck = session.dsl().resultQuery("""
                    select id, detail::JSON::STRUCT
                    (id INTEGER, name VARCHAR, role VARCHAR[], detail STRUCT(age INTEGER, address VARCHAR))
                    as detail from rq_mysql
                    """);
            session.jdbc("rq_duck", rq_duck);

            return session.dsl().resultQuery("""
                    select rq_duck.id, rq_duck.detail::JSON as detail, parquet.o_custkey, parquet.o_clerk, parquet.o_totalprice from rq_duck
                    left join 'D:/WORK/result.orders.parquet' as parquet on
                    rq_duck.detail.id = parquet.o_custkey
                    order by parquet.o_totalprice desc
                    """).fetch();
        });

        System.out.println(ret.format());
    }

    @Test
    public void dremio() throws SQLException {
        var ret = dremio.resultQuery("""
                SELECT id,name,age,detail,role,orders.o_totalprice FROM mongo.linkerp.staff,"@admin".orders
                WHERE staff.id = orders.o_custkey and array_length(staff.role) > 0
                """).fetch();
        System.out.println(ret.format());
    }
}
