/**
 * Created by heathhose on 17-3-2.
 */
import org.junit.Test;

import java.util.List;

public class MyTest {

    @org.junit.Test
    public void test(){
        String pullingUrls = "http://10.0.216.107:8080/zstack/v1/api-jobs/d113ca33b520468c952bc3f62ad061b5";

        String str ="http://172.20.12.9:8080";
        String[] strings = pullingUrls.split("http://10.0.216.107:8080");
        pullingUrls =str+strings[1];
        System.out.println(pullingUrls);
    }
}
