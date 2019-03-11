package io.yggdrash.validator.service.pbft;

import io.yggdrash.validator.YggdrashValidator;
import io.yggdrash.validator.data.pbft.PbftBlockChain;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static io.yggdrash.common.util.Utils.sleep;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(classes = YggdrashValidator.class)
public class PbftServiceTest {

    private final PbftBlockChain blockChain;
    private final PbftService pbftService;

    @Autowired
    public PbftServiceTest(PbftBlockChain blockChain, PbftService pbftService) {
        this.blockChain = blockChain;
        this.pbftService = pbftService;
    }

    @Test
    public void checkNodeTest() {
        for (int i = 0; i < 1000; i++) {
            pbftService.checkNode();
        }

        System.gc();
        sleep(3600000);
    }
}
