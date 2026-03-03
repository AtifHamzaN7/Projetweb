package n7.facade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdherentController.class)
class AdherentControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdherentRepository adherentRepository;

    @Test
    void createAdherent_persistsAndReturnsJson() throws Exception {
        Adherent saved = new Adherent();
        saved.setIdAdh(1);
        saved.setNom("N");
        when(adherentRepository.save(any(Adherent.class))).thenReturn(saved);

        mockMvc.perform(post("/api/adherents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"N\",\"prenom\":\"P\",\"email\":\"e\",\"password\":\"pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAdh").value(1))
                .andExpect(jsonPath("$.nom").value("N"));
    }

    @Test
    void inscription_returnsSavedAdherent() throws Exception {
        Adherent saved = new Adherent();
        saved.setIdAdh(2);
        saved.setNom("Doe");
        when(adherentRepository.save(any(Adherent.class))).thenReturn(saved);

        mockMvc.perform(post("/api/adherents/adherents/inscription")
                        .param("nom", "Doe")
                        .param("prenom", "John")
                        .param("email", "a@b.com")
                        .param("password", "pw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAdh").value(2))
                .andExpect(jsonPath("$.nom").value("Doe"));
    }

    @Test
    void getAllAdherents_returnsList() throws Exception {
        Adherent a = new Adherent();
        a.setIdAdh(1);
        when(adherentRepository.findAll()).thenReturn(List.of(a));

        mockMvc.perform(get("/api/adherents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idAdh").value(1));
    }

    @Test
    void login_delegatesToRepository() throws Exception {
        Adherent a = new Adherent();
        a.setIdAdh(3);
        when(adherentRepository.findByEmailAndPassword("e", "p")).thenReturn(a);

        mockMvc.perform(get("/api/adherents/connexion")
                        .param("email", "e")
                        .param("password", "p"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAdh").value(3));
    }

    @Test
    void getAdherentById_returnsEntityOrNull() throws Exception {
        Adherent a = new Adherent();
        a.setIdAdh(7);
        a.setNom("X");
        when(adherentRepository.findById(7)).thenReturn(Optional.of(a));

        mockMvc.perform(get("/api/adherents/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("X"));
    }

    @Test
    void deleteAdherent_callsRepository() throws Exception {
        mockMvc.perform(delete("/api/adherents/5"))
                .andExpect(status().isOk());
        verify(adherentRepository).deleteById(5);
    }

    @Test
    void updateAdherent_updatesWhenFound() throws Exception {
        Adherent existing = new Adherent();
        existing.setIdAdh(1);
        existing.setNom("Old");
        when(adherentRepository.findById(1)).thenReturn(Optional.of(existing));
        when(adherentRepository.save(any(Adherent.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/adherents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"New\",\"prenom\":\"P\",\"email\":\"e\",\"password\":\"pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("New"));
    }

    @Test
    void updateAdherent_returnsNullWhenNotFound() throws Exception {
        when(adherentRepository.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/adherents/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"New\",\"prenom\":\"P\",\"email\":\"e\",\"password\":\"pw\"}"))
                .andExpect(status().isOk());
    }
}
