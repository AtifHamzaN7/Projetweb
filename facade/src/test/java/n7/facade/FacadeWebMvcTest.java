package n7.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(Facade.class)
class FacadeWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdherentRepository adherentRepository;
    @MockBean
    RecetteRepository recetteRepository;
    @MockBean
    EventRepository eventRepository;
    @MockBean
    IngredientRepository ingredientRepository;
    @MockBean
    CommentRepository commentRepository;
    @MockBean
    DiscussionRepository discussionRepository;
    @MockBean
    MessageRepository messageRepository;

    @Test
    void inscriptionAdherent_savesAdherent() throws Exception {
        when(adherentRepository.save(any(Adherent.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/adherents/inscription")
                        .param("nom", "Doe")
                        .param("prenom", "John")
                        .param("email", "john@doe.com")
                        .param("password", "secret"))
                .andExpect(status().isOk());

        ArgumentCaptor<Adherent> captor = ArgumentCaptor.forClass(Adherent.class);
        verify(adherentRepository).save(captor.capture());
        assertThat(captor.getValue().getNom()).isEqualTo("Doe");
        assertThat(captor.getValue().getPrenom()).isEqualTo("John");
        assertThat(captor.getValue().getEmail()).isEqualTo("john@doe.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("secret");
    }

    @Test
    void listeAdherents_returnsJsonArray() throws Exception {
        Adherent a1 = new Adherent();
        a1.setIdAdh(1);
        a1.setNom("A");
        Adherent a2 = new Adherent();
        a2.setIdAdh(2);
        a2.setNom("B");
        when(adherentRepository.findAll()).thenReturn(List.of(a1, a2));

        mockMvc.perform(get("/adherents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idAdh").value(1))
                .andExpect(jsonPath("$[1].idAdh").value(2));
    }

    @Test
    void miseAJourAdherent_updatesWhenFound() throws Exception {
        Adherent existing = new Adherent();
        existing.setIdAdh(10);
        existing.setNom("Old");
        when(adherentRepository.findById(10)).thenReturn(Optional.of(existing));
        when(adherentRepository.save(any(Adherent.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/adherents/mise-a-jour")
                        .param("idAdh", "10")
                        .param("nom", "New")
                        .param("prenom", "P")
                        .param("email", "n@e.com")
                        .param("password", "pwd"))
                .andExpect(status().isOk());

        ArgumentCaptor<Adherent> captor = ArgumentCaptor.forClass(Adherent.class);
        verify(adherentRepository).save(captor.capture());
        assertThat(captor.getValue().getNom()).isEqualTo("New");
        assertThat(captor.getValue().getEmail()).isEqualTo("n@e.com");
    }

    @Test
    void miseAJourAdherent_doesNothingWhenNotFound() throws Exception {
        when(adherentRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(post("/adherents/mise-a-jour")
                        .param("idAdh", "999")
                        .param("nom", "X")
                        .param("prenom", "Y")
                        .param("email", "x@y.com")
                        .param("password", "pwd"))
                .andExpect(status().isOk());

        verify(adherentRepository, never()).save(any());
    }

    @Test
    void suppressionAdherent_deletesById() throws Exception {
        mockMvc.perform(delete("/adherents/suppression/7"))
                .andExpect(status().isOk());
        verify(adherentRepository).deleteById(7);
    }

    @Test
    void getAdherentById_returnsAdherentOrNull() throws Exception {
        Adherent a = new Adherent();
        a.setIdAdh(3);
        a.setNom("Nom");
        when(adherentRepository.findById(3)).thenReturn(Optional.of(a));

        mockMvc.perform(get("/adherents/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAdh").value(3))
                .andExpect(jsonPath("$.nom").value("Nom"));
    }

    @Test
    void connexionAdherent_delegatesToRepository() throws Exception {
        Adherent a = new Adherent();
        a.setIdAdh(9);
        when(adherentRepository.findByEmailAndPassword("e", "p")).thenReturn(a);

        mockMvc.perform(get("/adherents/connexion")
                        .param("email", "e")
                        .param("password", "p"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAdh").value(9));
    }

    @Test
    void ajoutRecette_parsesIngredientsAndSavesRecette() throws Exception {
        Adherent auteur = new Adherent();
        auteur.setIdAdh(5);
        when(adherentRepository.findById(5)).thenReturn(Optional.of(auteur));
        when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(inv -> inv.getArgument(0));
        when(recetteRepository.save(any(Recette.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/recettes/ajout")
                        .param("nom", "Cake")
                        .param("ingredients", "(sucre,100,10g),(sel,0,1g)")
                        .param("etapes", "step1", "step2")
                        .param("photo", "img.jpg")
                        .param("auteurId", "5")
                        .param("categories", "vegan", "sans gluten"))
                .andExpect(status().isOk());

        ArgumentCaptor<Recette> recetteCaptor = ArgumentCaptor.forClass(Recette.class);
        verify(recetteRepository).save(recetteCaptor.capture());
        Recette saved = recetteCaptor.getValue();
        assertThat(saved.getNom()).isEqualTo("Cake");
        assertThat(saved.getAuteur()).isSameAs(auteur);
        assertThat(saved.getIngredients()).hasSize(2);
        assertThat(saved.getIngredients().get(0).getNom()).isEqualTo("sucre");
        assertThat(saved.getIngredients().get(0).getCalories()).isEqualTo(100);
        assertThat(saved.getIngredients().get(0).getQuantite()).isEqualTo("10g");
        assertThat(saved.getCategories()).containsExactly("vegan", "sans gluten");
    }

    @Test
    void ajoutRecette_returns500WhenAuteurMissing() throws Exception {
        when(adherentRepository.findById(123)).thenReturn(Optional.empty());

        ServletException ex = Assertions.assertThrows(ServletException.class, () ->
            mockMvc.perform(post("/recettes/ajout")
                    .param("nom", "Cake")
                    .param("ingredients", "(sucre,100,10g)")
                    .param("etapes", "step1")
                    .param("auteurId", "123")
                    .param("categories", "vegan"))
                .andReturn()
        );
        assertThat(ex.getRootCause()).isInstanceOf(IllegalArgumentException.class);
        assertThat(ex.getRootCause()).hasMessageContaining("Auteur introuvable");
    }

    @Test
    void getRecettesByAdherent_returnsList() throws Exception {
        Adherent adherent = new Adherent();
        adherent.setIdAdh(2);
        when(adherentRepository.findById(2)).thenReturn(Optional.of(adherent));
        Recette r = new Recette();
        r.setIdRec(11);
        r.setNom("R");
        when(recetteRepository.findByAuteur_IdAdh(2)).thenReturn(List.of(r));

        mockMvc.perform(get("/adherents/2/recettes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idRec").value(11));
    }

    @Test
    void ajoutEvenement_savesEvent() throws Exception {
        Adherent auteur = new Adherent();
        auteur.setIdAdh(4);
        when(adherentRepository.findById(4)).thenReturn(Optional.of(auteur));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/evenements/ajout")
                        .param("titre", "Meet")
                        .param("date", "2026-03-03")
                        .param("lieu", "Toulouse")
                        .param("description", "Desc")
                        .param("auteurId", "4"))
                .andExpect(status().isOk());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getTitre()).isEqualTo("Meet");
        assertThat(captor.getValue().getAuteur()).isSameAs(auteur);
        assertThat(captor.getValue().getParticipants()).contains(auteur);
    }

    @Test
    void participer_addsParticipantAndSaves() throws Exception {
        Event event = new Event();
        event.setId(1);
        Adherent adherent = new Adherent();
        adherent.setIdAdh(2);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(adherentRepository.findById(2)).thenReturn(Optional.of(adherent));

        mockMvc.perform(post("/evenements/1/participer")
                        .param("adherentId", "2"))
                .andExpect(status().isOk());

        assertThat(event.getParticipants()).contains(adherent);
        verify(eventRepository).save(eq(event));
    }

    @Test
    void ajouterCommentaire_savesComment() throws Exception {
        Recette recette = new Recette();
        recette.setIdRec(7);
        Adherent auteur = new Adherent();
        auteur.setIdAdh(8);
        when(recetteRepository.findById(7)).thenReturn(Optional.of(recette));
        when(adherentRepository.findById(8)).thenReturn(Optional.of(auteur));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/recettes/7/commentaires/ajout")
                        .param("auteurId", "8")
                        .param("content", "Hello"))
                .andExpect(status().isOk());

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("Hello");
        assertThat(captor.getValue().getAuteur()).isSameAs(auteur);
        assertThat(captor.getValue().getRecette()).isSameAs(recette);
    }

    @Test
    void getCommentairesByRecette_returnsList() throws Exception {
        Comment c = new Comment();
        c.setIdCom(1);
        c.setContent("C");
        when(commentRepository.findByRecette_IdRec(9)).thenReturn(List.of(c));

        mockMvc.perform(get("/recettes/9/commentaires"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idCom").value(1))
                .andExpect(jsonPath("$[0].content").value("C"));
    }

    @Test
    void ajouterDiscussion_savesDiscussion() throws Exception {
        Adherent auteur = new Adherent();
        auteur.setIdAdh(1);
        when(adherentRepository.findById(1)).thenReturn(Optional.of(auteur));
        when(discussionRepository.save(any(Discussion.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/discussions/ajout")
                        .param("titre", "T")
                        .param("question", "Q")
                        .param("auteurId", "1"))
                .andExpect(status().isOk());

        ArgumentCaptor<Discussion> captor = ArgumentCaptor.forClass(Discussion.class);
        verify(discussionRepository).save(captor.capture());
        assertThat(captor.getValue().getTitre()).isEqualTo("T");
        assertThat(captor.getValue().getAuteur()).isSameAs(auteur);
    }

    @Test
    void ajouterMessage_savesMessage() throws Exception {
        Discussion discussion = new Discussion();
        discussion.setIdDisc(3);
        Adherent auteur = new Adherent();
        auteur.setIdAdh(4);
        when(discussionRepository.findById(3)).thenReturn(Optional.of(discussion));
        when(adherentRepository.findById(4)).thenReturn(Optional.of(auteur));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/discussions/3/messages/ajout")
                        .param("auteurId", "4")
                        .param("content", "Hi"))
                .andExpect(status().isOk());

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("Hi");
        assertThat(captor.getValue().getAuteur()).isSameAs(auteur);
        assertThat(captor.getValue().getDiscussion()).isSameAs(discussion);
    }

    @Test
    void getMessagesByDiscussion_returnsDiscussionMessages() throws Exception {
        Discussion discussion = new Discussion();
        discussion.setIdDisc(1);
        Message m = new Message();
        m.setIdMsg(2);
        m.setContent("Hello");
        discussion.setMessages(List.of(m));
        when(discussionRepository.findById(1)).thenReturn(Optional.of(discussion));

        mockMvc.perform(get("/discussions/1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idMsg").value(2))
                .andExpect(jsonPath("$[0].content").value("Hello"));
    }

    @Test
    void getImage_returns404WhenMissing() throws Exception {
        mockMvc.perform(get("/images/does-not-exist.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getImage_servesFromClasspathStatic() throws Exception {
        mockMvc.perform(get("/images/test.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void getParticipationsByAdherent_returnsEvents() throws Exception {
        Adherent adherent = new Adherent();
        adherent.setIdAdh(9);
        when(adherentRepository.findById(9)).thenReturn(Optional.of(adherent));
        Event e = new Event();
        e.setId(1);
        e.setTitre("T");
        e.setDate(new Date());
        when(eventRepository.findByParticipantId(9)).thenReturn(List.of(e));

        mockMvc.perform(get("/adherents/9/participations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getMessagesByAdherent_returnsMessages() throws Exception {
        Adherent adherent = new Adherent();
        adherent.setIdAdh(7);
        when(adherentRepository.findById(7)).thenReturn(Optional.of(adherent));
        Message m = new Message();
        m.setIdMsg(1);
        m.setContent("C");
        when(messageRepository.findByAuteur_IdAdh(7)).thenReturn(List.of(m));

        mockMvc.perform(get("/adherents/7/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idMsg").value(1));
    }
}
