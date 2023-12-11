import org.jooq.exception.DataAccessException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.umbrella.query.QueryApplication;
import org.umbrella.query.QueryEngine;
import org.umbrella.query.reader.ArrowJSONReader;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = QueryApplication.class)
public class WindowsQueryEngineTest {

    @Autowired
    private QueryEngine engine;

    @Test
    public void json() {
        try(var reader = new ArrowJSONReader(engine.allocator(), engine.memoryPool(), "file:/D:/WORK/data.json")){
            while(reader.loadNextBatch()) {
                try(var root = reader.getVectorSchemaRoot()) {
                    System.out.println(root.contentToTSVString());
                }
            }
        } catch (IOException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    @Test
    public void json_engine() {
        var ret = engine.json("data", "file:/D:/WORK/data.json",
                ctx -> ctx.resultQuery("select b from data where info.name = 'Mike' or info.age=42").fetch());
        System.out.println(ret.format());
    }

    @Test
    public void json_duckdb() {
        var ret = engine.duckdb().resultQuery("""
                select b from 'D:/WORK/data.json' where info.name = 'Mike' or info.age=42
                """).fetch();
        System.out.println(ret.format());
    }
}
