package teste.delta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import teste.delta.repository.TransferenciaRepository;
import teste.delta.service.TransferenciaService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith (MockitoExtension.class)
public class TransferenciaServiceTest {

    @Mock 
    private TransferenciaRepository transferenciaRepository;

    private final TransferenciaService transferenciaService = new TransferenciaService();

    @Test 
    void devecalculateRateDeTransferencia(){
        int resultFreeRate = ReflectionTestUtils.invokeMethod(
            transferenciaService, "calculateRate", 1000);
            
        assertThat(0).isEqualByComparingTo(resultFreeRate);

        int resultLimitWithoutRate = ReflectionTestUtils.invokeMethod(
            transferenciaService, "calculateRate", 10000);
            
        assertThat(0).isEqualByComparingTo(resultLimitWithoutRate);

        int resultRateBase = ReflectionTestUtils.invokeMethod(
            transferenciaService, "calculateRate", 10001);
            
        assertThat(100).isEqualByComparingTo(resultRateBase);

        int resultCalcRate = ReflectionTestUtils.invokeMethod(
            transferenciaService, "calculateRate", 11250);
            
        assertThat(113).isEqualByComparingTo(resultCalcRate);

        int resultCalcRate150 = ReflectionTestUtils.invokeMethod(
            transferenciaService, "calculateRate", 15000);
            
        assertThat(150).isEqualByComparingTo(resultCalcRate150);

        int resultCalcRate123456 = ReflectionTestUtils.invokeMethod(
            transferenciaService, "calculateRate", 123456);
            
        assertThat(1235).isEqualByComparingTo(resultCalcRate123456);

        int resultBaseOfLimit = ReflectionTestUtils.invokeMethod(
            transferenciaService, "calculateRate", 200000);
            
        assertThat(2000).isEqualByComparingTo(resultBaseOfLimit);

        int resultCalcAboveCeiling = ReflectionTestUtils.invokeMethod(
            transferenciaService, "calculateRate", 500000);
            
        assertThat(2000).isEqualByComparingTo(resultCalcAboveCeiling);

        
    }
}
