package com.example.demo;

import com.example.demo.model.Conta;
import com.example.demo.repository.ContaRepository;
import com.example.demo.repository.MovimentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DemoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ContaRepository contaRepository;

	@Autowired
	private MovimentoRepository movimentoRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void deveRealizarDeposito() throws Exception {

		Conta conta = contaRepository.findById(3L)
				.orElseThrow();

		long saldoAntes = conta.getSaldoCentavos();

		mockMvc.perform(
						post("/contas/" + conta.getNumero() + "/depositos")
								.header("Idempotency-Key", "teste-deposito-001")
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
                                {
                                    "valorCentavos": 10000
                                }
                                """)
				)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.numero").value(conta.getNumero()))
				.andExpect(jsonPath("$.saldoCentavos").value(saldoAntes + 10000));

		Conta contaDepois = contaRepository.findById(3L)
				.orElseThrow();

		assertEquals(
				saldoAntes + 10000,
				contaDepois.getSaldoCentavos()
		);
	}
}