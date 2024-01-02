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
    public void read_1(){

    }

}
