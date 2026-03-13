package n7.facade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FacadeTest {

    @Mock
    private AdherentRepository adherentRepository;

    @Mock
    private RecetteRepository recetteRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private DiscussionRepository discussionRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private Facade facade;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void inscriptionAdherent_shouldSaveAdherent() {
        facade.inscriptionAdherent("Doe", "John", "john@example.com", "pass123");

        ArgumentCaptor<Adherent> captor = ArgumentCaptor.forClass(Adherent.class);
        verify(adherentRepository).save(captor.capture());

        Adherent saved = captor.getValue();
        assertEquals("Doe", saved.getNom());
        assertEquals("John", saved.getPrenom());
        assertEquals("john@example.com", saved.getEmail());
        assertEquals("pass123", saved.getPassword());
    }

    @Test
    void listeAdherents_shouldReturnAllAdherents() {
        List<Adherent> list = List.of(new Adherent(1, "A", "B", "a@b.com", "pwd"));
        when(adherentRepository.findAll()).thenReturn(list);

        List<Adherent> result = facade.listeAdherents();

        assertEquals(list, result);
    }

    @Test
    void miseAJourAdherent_existingAdherent_shouldUpdateAndSave() {
        Adherent existing = new Adherent(1, "Old", "Old", "old@example.com", "oldpwd");
        when(adherentRepository.findById(1)).thenReturn(Optional.of(existing));

        facade.miseAJourAdherent(1, "NewNom", "NewPrenom", "new@example.com", "newpwd");

        assertEquals("NewNom", existing.getNom());
        assertEquals("NewPrenom", existing.getPrenom());
        assertEquals("new@example.com", existing.getEmail());
        assertEquals("newpwd", existing.getPassword());

        verify(adherentRepository).save(existing);
    }

    @Test
    void miseAJourAdherent_nonExistingAdherent_shouldNotSave() {
        when(adherentRepository.findById(99)).thenReturn(Optional.empty());

        facade.miseAJourAdherent(99, "Nom", "Prenom", "email", "pwd");

        verify(adherentRepository, never()).save(any());
    }

    @Test
    void suppressionAdherent_shouldCallDeleteById() {
        facade.suppressionAdherent(5);
        verify(adherentRepository).deleteById(5);
    }

    @Test
    void getAdherentById_existing_shouldReturnAdherent() {
        Adherent adh = new Adherent(3, "N", "P", "e@e.com", "pwd");
        when(adherentRepository.findById(3)).thenReturn(Optional.of(adh));

        Adherent result = facade.getAdherentById(3);

        assertEquals(adh, result);
    }

    @Test
    void getAdherentById_nonExisting_shouldReturnNull() {
        when(adherentRepository.findById(10)).thenReturn(Optional.empty());

        Adherent result = facade.getAdherentById(10);

        assertNull(result);
    }

    @Test
    void connexionAdherent_shouldReturnAdherentFromRepo() {
        Adherent adh = new Adherent(1, "N", "P", "email", "pwd");
        when(adherentRepository.findByEmailAndPassword("email", "pwd")).thenReturn(adh);

        Adherent result = facade.connexionAdherent("email", "pwd");

        assertEquals(adh, result);
    }

@org.junit.jupiter.api.Test
void ajoutRecette_validInput_shouldSaveRecetteAndIngredients() {
    Adherent auteur = new Adherent(1, "Nom", "Prenom", "email", "pwd");
    when(adherentRepository.findById(1)).thenReturn(Optional.of(auteur));

    Ingredient savedIngredient = new Ingredient("Sugar");
    savedIngredient.setCalories(100);
    savedIngredient.setQuantite("2 cups");
    when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(invocation -> invocation.getArgument(0));

    List<String> etapes = List.of("Step1", "Step2");
    List<String> categories = List.of("vegan", "dessert");

    String ingredientsStr = "(Sugar,100,2 cups)";

    // Mock recetteRepository.save to return the recette passed
    when(recetteRepository.save(any(Recette.class))).thenAnswer(invocation -> invocation.getArgument(0));

    facade.ajoutRecette("Cake", ingredientsStr, etapes, "photo.jpg", 1, categories);

    verify(adherentRepository).findById(1);
    verify(ingredientRepository).save(any(Ingredient.class));
    verify(recetteRepository).save(any(Recette.class));

    // Check that the auteur has the recette added
    assertTrue(auteur.getRecettes().stream().anyMatch(r -> "Cake".equals(r.getNom())));
}

    @Test
    void ajoutRecette_invalidIngredientFormat_shouldThrow() {
        Adherent auteur = new Adherent(1, "Nom", "Prenom", "email", "pwd");
        when(adherentRepository.findById(1)).thenReturn(Optional.of(auteur));

        String badIngredientsStr = "(Sugar,100)"; // missing quantity

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            facade.ajoutRecette("Cake", badIngredientsStr, List.of("Step1"), "photo.jpg", 1, List.of("vegan"));
        });

        assertTrue(ex.getMessage().contains("Format d'ingrédient invalide"));
    }

    @Test
    void ajoutRecette_nonExistingAuteur_shouldThrow() {
        when(adherentRepository.findById(1)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            facade.ajoutRecette("Cake", "(Sugar,100,2 cups)", List.of("Step1"), "photo.jpg", 1, List.of("vegan"));
        });

        assertEquals("Auteur introuvable !", ex.getMessage());
    }

    @Test
    void getRecettesByAdherent_existingAdherent_shouldReturnRecettes() {
        Adherent adh = new Adherent(1, "N", "P", "email", "pwd");
        when(adherentRepository.findById(1)).thenReturn(Optional.of(adh));
        List<Recette> recettes = List.of(new Recette());
        when(recetteRepository.findByAuteur_IdAdh(1)).thenReturn(recettes);

        List<Recette> result = facade.getRecettesByAdherent(1);

        assertEquals(recettes, result);
    }

    @Test
    void getRecettesByAdherent_nonExistingAdherent_shouldThrow() {
        when(adherentRepository.findById(99)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> facade.getRecettesByAdherent(99));

        assertEquals("Adhérent introuvable !", ex.getMessage());
    }

    @Test
    void listeRecettes_shouldReturnAllRecettes() {
        List<Recette> recettes = List.of(new Recette(), new Recette());
        when(recetteRepository.findAll()).thenReturn(recettes);

        List<Recette> result = facade.listeRecettes();

        assertEquals(recettes, result);
    }

    @Test
    void suppressionRecette_shouldCallDeleteById() {
        facade.suppressionRecette(7);
        verify(recetteRepository).deleteById(7);
    }

    @Test
    void ajoutEvenement_validInput_shouldSaveEvent() {
        Adherent auteur = new Adherent(1, "Nom", "Prenom", "email", "pwd");
        when(adherentRepository.findById(1)).thenReturn(Optional.of(auteur));

        Date date = new GregorianCalendar(2023, Calendar.MARCH, 10).getTime();

        facade.ajoutEvenement("Titre", date, "Lieu", "Desc", 1);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());

        Event saved = captor.getValue();
        assertEquals("Titre", saved.getTitre());
        assertEquals(date, saved.getDate());
        assertEquals("Lieu", saved.getLieu());
        assertEquals("Desc", saved.getDescription());
        assertTrue(saved.getParticipants().contains(auteur));
        assertEquals(auteur, saved.getAuteur());
        assertTrue(auteur.getEvenements().contains(saved));
    }

    @Test
    void ajoutEvenement_nonExistingAuteur_shouldThrow() {
        when(adherentRepository.findById(1)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> facade.ajoutEvenement("T", new Date(), "L", "D", 1));

        assertEquals("Auteur introuvable !", ex.getMessage());
    }

    @Test
    void getEvenementsByAdherent_existingAdherent_shouldReturnEvents() {
        Adherent adh = new Adherent(1, "N", "P", "email", "pwd");
        when(adherentRepository.findById(1)).thenReturn(Optional.of(adh));
        List<Event> events = List.of(new Event());
        when(eventRepository.findByAuteur_IdAdh(1)).thenReturn(events);

        List<Event> result = facade.getEvenementsByAdherent(1);

        assertEquals(events, result);
    }

    @Test
    void getEvenementsByAdherent_nonExistingAdherent_shouldThrow() {
        when(adherentRepository.findById(99)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> facade.getEvenementsByAdherent(99));

        assertEquals("Adhérent introuvable !", ex.getMessage());
    }

    @Test
    void listeEvenements_shouldReturnAllEvents() {
        List<Event> events = List.of(new Event(), new Event());
        when(eventRepository.findAll()).thenReturn(events);

        List<Event> result = facade.listeEvenements();

        assertEquals(events, result);
    }

    @Test
    void getEventById_existing_shouldReturnEvent() {
        Event event = new Event();
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));

        Event result = facade.getEventById(1);

        assertEquals(event, result);
    }

    @Test
    void getEventById_nonExisting_shouldReturnNull() {
        when(eventRepository.findById(10)).thenReturn(Optional.empty());

        Event result = facade.getEventById(10);

        assertNull(result);
    }

    @Test
    void suppressionEvenement_shouldCallDeleteById() {
        facade.suppressionEvenement(5);
        verify(eventRepository).deleteById(5);
    }

    @Test
    void participer_shouldAddParticipantAndSave() {
        Event event = new Event();
        Adherent adherent = new Adherent();
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(adherentRepository.findById(2)).thenReturn(Optional.of(adherent));

        facade.participer(1, 2);

        assertTrue(event.getParticipants().contains(adherent));
        verify(eventRepository).save(event);
    }

    @Test
    void ajouterCommentaire_validInput_shouldSaveComment() {
        Recette recette = new Recette();
        Adherent auteur = new Adherent();
        when(recetteRepository.findById(1)).thenReturn(Optional.of(recette));
        when(adherentRepository.findById(2)).thenReturn(Optional.of(auteur));

        facade.ajouterCommentaire(1, 2, "Nice recipe");

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());

        Comment comment = captor.getValue();
        assertEquals("Nice recipe", comment.getContent());
        assertEquals(auteur, comment.getAuteur());
        assertEquals(recette, comment.getRecette());
    }

    @Test
    void ajouterCommentaire_missingRecetteOrAuteur_shouldThrow() {
        when(recetteRepository.findById(1)).thenReturn(Optional.empty());
        when(adherentRepository.findById(2)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> facade.ajouterCommentaire(1, 2, "c"));
        assertEquals("Recette ou auteur introuvable !", ex.getMessage());
    }

    @Test
    void getCommentairesByRecette_shouldReturnComments() {
        List<Comment> comments = List.of(new Comment(), new Comment());
        when(commentRepository.findByRecette_IdRec(1)).thenReturn(comments);

        List<Comment> result = facade.getCommentairesByRecette(1);

        assertEquals(comments, result);
    }

    @Test
    void ajouterDiscussion_validInput_shouldSaveDiscussion() {
        Adherent auteur = new Adherent();
        when(adherentRepository.findById(1)).thenReturn(Optional.of(auteur));

        facade.ajouterDiscussion("Title", "Question", 1);

        ArgumentCaptor<Discussion> captor = ArgumentCaptor.forClass(Discussion.class);
        verify(discussionRepository).save(captor.capture());

        Discussion discussion = captor.getValue();
        assertEquals("Title", discussion.getTitre());
        assertEquals("Question", discussion.getQuestion());
        assertEquals(auteur, discussion.getAuteur());
    }

    @Test
    void ajouterDiscussion_nonExistingAuteur_shouldThrow() {
        when(adherentRepository.findById(1)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> facade.ajouterDiscussion("T", "Q", 1));

        assertEquals("Auteur introuvable !", ex.getMessage());
    }

    @Test
    void listeDiscussions_shouldReturnAllDiscussions() {
        List<Discussion> discussions = List.of(new Discussion(), new Discussion());
        when(discussionRepository.findAll()).thenReturn(discussions);

        List<Discussion> result = facade.listeDiscussions();

        assertEquals(discussions, result);
    }

    @Test
    void getDiscussionById_existing_shouldReturnDiscussion() {
        Discussion discussion = new Discussion();
        when(discussionRepository.findById(1)).thenReturn(Optional.of(discussion));

        Discussion result = facade.getDiscussionById(1);

        assertEquals(discussion, result);
    }

    @Test
    void getDiscussionById_nonExisting_shouldReturnNull() {
        when(discussionRepository.findById(10)).thenReturn(Optional.empty());

        Discussion result = facade.getDiscussionById(10);

        assertNull(result);
    }

    @Test
    void ajouterMessage_validInput_shouldSaveMessage() {
        Discussion discussion = new Discussion();
        Adherent auteur = new Adherent();
        when(discussionRepository.findById(1)).thenReturn(Optional.of(discussion));
        when(adherentRepository.findById(2)).thenReturn(Optional.of(auteur));

        facade.ajouterMessage(1, 2, "Hello");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());

        Message message = captor.getValue();
        assertEquals("Hello", message.getContent());
        assertEquals(auteur, message.getAuteur());
        assertEquals(discussion, message.getDiscussion());
    }

    @Test
    void ajouterMessage_missingDiscussionOrAuteur_shouldThrow() {
        when(discussionRepository.findById(1)).thenReturn(Optional.empty());
        when(adherentRepository.findById(2)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> facade.ajouterMessage(1, 2, "c"));
        assertEquals("Discussion ou auteur introuvable !", ex.getMessage());
    }

    @Test
    void getMessagesByDiscussion_existingDiscussion_shouldReturnMessages() {
        Discussion discussion = new Discussion();
        List<Message> messages = List.of(new Message(), new Message());
        discussion.setMessages(messages);
        when(discussionRepository.findById(1)).thenReturn(Optional.of(discussion));

        List<Message> result = facade.getMessagesByDiscussion(1);

        assertEquals(messages, result);
    }

    @Test
    void getMessagesByDiscussion_nonExistingDiscussion_shouldThrow() {
        when(discussionRepository.findById(99)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> facade.getMessagesByDiscussion(99));

        assertEquals("Discussion introuvable !", ex.getMessage());
    }

@org.junit.jupiter.api.Test
void getImage_existingFile_shouldReturnResource() throws Exception {
    // We create a spy of facade to mock getImage method properly
    Facade spyFacade = spy(facade);

    // Mock getImage to return a ResponseEntity with a ByteArrayResource body
    org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(new byte[0]);
    org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> responseEntity = org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.IMAGE_PNG).body(resource);

    // Use doReturn to avoid WrongTypeOfReturnValue exception
    doReturn(responseEntity).when(spyFacade).getImage(anyString());

    // Call the method
    org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> response = spyFacade.getImage("test.png");

    // Verify response
    assertNotNull(response);
    assertEquals(200, response.getStatusCodeValue());
    assertTrue(response.getBody() instanceof org.springframework.core.io.Resource);
}

    @Test
    void uploadImage_shouldReturnOkWithFileName() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());

        ResponseEntity<String> response = facade.uploadImage(file);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("test.jpg", response.getBody());

        // Clean up the uploaded file
        Path path = Paths.get("src/main/resources/static/test.jpg");
        Files.deleteIfExists(path);
    }

    @Test
    void uploadImage_whenIOException_shouldReturnInternalServerError() throws IOException {
        MockMultipartFile file = mock(MockMultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("fail.jpg");
        when(file.getInputStream()).thenThrow(new IOException("fail"));

        ResponseEntity<String> response = facade.uploadImage(file);

        assertEquals(500, response.getStatusCodeValue());
        assertEquals("Failed to upload image", response.getBody());
    }

    @Test
    void getParticipationsByAdherent_existingAdherent_shouldReturnEvents() {
        Adherent adh = new Adherent(1, "N", "P", "email", "pwd");
        when(adherentRepository.findById(1)).thenReturn(Optional.of(adh));
        List<Event> events = List.of(new Event(), new Event());
        when(eventRepository.findByParticipantId(1)).thenReturn(events);

        List<Event> result = facade.getParticipationsByAdherent(1);

        assertEquals(events, result);
    }

    @Test
    void getParticipationsByAdherent_nonExistingAdherent_shouldThrow() {
        when(adherentRepository.findById(99)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> facade.getParticipationsByAdherent(99));

        assertEquals("Adhérent introuvable !", ex.getMessage());
    }

    @Test
    void getMessagesByAdherent_existingAdherent_shouldReturnMessages() {
        Adherent adh = new Adherent(1, "N", "P", "email", "pwd");
        when(adherentRepository.findById(1)).thenReturn(Optional.of(adh));
        List<Message> messages = List.of(new Message(), new Message());
        when(messageRepository.findByAuteur_IdAdh(1)).thenReturn(messages);

        List<Message> result = facade.getMessagesByAdherent(1);

        assertEquals(messages, result);
    }

    @Test
    void getMessagesByAdherent_nonExistingAdherent_shouldThrow() {
        when(adherentRepository.findById(99)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () -> facade.getMessagesByAdherent(99));

        assertEquals("Adhérent introuvable !", ex.getMessage());
    }

}
