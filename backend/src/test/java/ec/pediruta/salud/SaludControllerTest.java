package ec.pediruta.salud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el controlador sin necesitar Docker: la base de datos se simula.
 */
@WebMvcTest(SaludController.class)
class SaludControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JdbcTemplate jdbc;

    @Test
    void conBaseDisponibleDevuelveVersionDePostgis() throws Exception {
        when(jdbc.queryForObject(anyString(), eq(String.class))).thenReturn("3.4 USE_GEOS=1");

        mvc.perform(get("/api/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseDeDatos").value("conectada"))
                .andExpect(jsonPath("$.postgis").value("3.4 USE_GEOS=1"));
    }

    @Test
    void conBaseCaidaNoDevuelveError500() throws Exception {
        when(jdbc.queryForObject(anyString(), eq(String.class)))
                .thenThrow(new DataAccessResourceFailureException("Connection refused"));

        mvc.perform(get("/api/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseDeDatos").value("sin conexión"))
                .andExpect(jsonPath("$.postgis").doesNotExist());
    }
}
