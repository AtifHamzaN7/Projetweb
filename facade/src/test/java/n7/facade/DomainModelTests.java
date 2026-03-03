package n7.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

class DomainModelTests {

    @Test
    void adherent_addRecette_setsAuteurAndAddsToList() {
        Adherent adherent = new Adherent();
        Recette recette = new Recette();

        adherent.addRecette(recette);

        assertThat(adherent.getRecettes()).contains(recette);
        assertThat(recette.getAuteur()).isSameAs(adherent);
    }

    @Test
    void adherent_addEvenement_setsAuteurAndAddsToList() {
        Adherent adherent = new Adherent();
        Event event = new Event();

        adherent.addEvenement(event);

        assertThat(adherent.getEvenements()).contains(event);
        assertThat(event.getAuteur()).isSameAs(adherent);
    }

    @Test
    void adherent_addDiscussion_setsAuteurAndAddsToList() {
        Adherent adherent = new Adherent();
        Discussion discussion = new Discussion();

        adherent.addDiscussion(discussion);

        assertThat(adherent.getDiscussions()).contains(discussion);
        assertThat(discussion.getAuteur()).isSameAs(adherent);
    }

    @Test
    void adherent_addIngredientHelpers_doNotDuplicate() {
        Adherent adherent = new Adherent();
        Ingredient ingredient = new Ingredient("Sucre");

        adherent.addIngredientToMyIngredients(ingredient);
        adherent.addIngredientToMyIngredients(ingredient);
        adherent.addIngredientToShoppingList(ingredient);
        adherent.addIngredientToShoppingList(ingredient);

        assertThat(adherent.getMyIngredients()).hasSize(1);
        assertThat(adherent.getShoppingList()).hasSize(1);
    }

    @Test
    void event_addParticipant_doesNotDuplicate() {
        Event event = new Event();
        Adherent participant = new Adherent();
        participant.setIdAdh(42);

        event.addParticipant(participant);
        event.addParticipant(participant);

        assertThat(event.getParticipants()).hasSize(1);
    }

    @Test
    void recette_addComment_setsBackReference() {
        Recette recette = new Recette();
        Comment comment = new Comment();

        recette.addComment(comment);

        assertThat(recette.getComments()).contains(comment);
        assertThat(comment.getRecette()).isSameAs(recette);
    }

    @Test
    void discussion_setAuteur_addsDiscussionToAuteur() {
        Adherent adherent = new Adherent();
        Discussion discussion = new Discussion();

        discussion.setAuteur(adherent);

        assertThat(discussion.getAuteur()).isSameAs(adherent);
        assertThat(adherent.getDiscussions()).contains(discussion);
    }

    @Test
    void message_setAuteur_addsMessageToAuteur() {
        Adherent adherent = new Adherent();
        Message message = new Message();

        message.setAuteur(adherent);

        assertThat(message.getAuteur()).isSameAs(adherent);
        assertThat(adherent.getMessages()).contains(message);
    }

    @Test
    void message_setDiscussion_addsMessageToDiscussion() {
        Discussion discussion = new Discussion();
        Message message = new Message();

        message.setDiscussion(discussion);

        assertThat(message.getDiscussion()).isSameAs(discussion);
        assertThat(discussion.getMessages()).contains(message);
    }

    @Test
    void comment_setAuteur_addsCommentToAuteur() {
        Adherent adherent = new Adherent();
        Comment comment = new Comment();

        comment.setAuteur(adherent);

        assertThat(comment.getAuteur()).isSameAs(adherent);
        assertThat(adherent.getComments()).contains(comment);
    }

    @Test
    void ingredient_setRecettes_syncsIngredientInRecettes() {
        Ingredient ingredient = new Ingredient("Sel");
        Recette recette1 = new Recette();
        Recette recette2 = new Recette();

        ingredient.setRecettes(List.of(recette1, recette2));

        assertThat(recette1.getIngredients()).contains(ingredient);
        assertThat(recette2.getIngredients()).contains(ingredient);
    }

    @Test
    void basicGettersSetters_smoke() {
        Adherent adherent = new Adherent(1, "Nom", "Prenom", "a@b.com", "pwd");
        assertThat(adherent.getIdAdh()).isEqualTo(1);
        assertThat(adherent.getNom()).isEqualTo("Nom");

        Event event = new Event(1, "Titre", new Date(), "Lieu", "Desc", adherent, List.of(adherent));
        assertThat(event.getId()).isEqualTo(1);
        assertThat(event.getTitre()).isEqualTo("Titre");
    }
}
