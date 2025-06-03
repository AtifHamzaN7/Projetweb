import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { AuthComponent } from './auth/auth.component';
import { ConnComponent } from './conn/conn.component';
import { AdherentComponent } from './adherent/adherent.component';
import { RecipesComponent } from './recipes/recipes.component';
import { RecipeDetailsComponent } from './recipe-details/recipe-details.component';
import { AddRecipeComponent } from './add-recipe/add-recipe.component';
import { EventsComponent } from './events/events.component';
import { AddEventComponent } from './add-event/add-event.component';
import { EventDetailComponent } from './event-detail/event-detail.component';
import { AProposComponent } from './a-propos/a-propos.component';
import { ContactComponent } from './contact/contact.component';


export const routes: Routes = [
  { path: 'about', component: AProposComponent },
  { path: 'contact', component: ContactComponent },
  { path: 'home', component: HomeComponent },
  { path: 'evenements', component: EventsComponent },
  { path: 'evenements/:id', loadComponent: () => import('./event-detail/event-detail.component').then(m => m.EventDetailComponent) },
   {path: 'auth',
    loadComponent: () => import('./auth/auth.component').then(m => m.AuthComponent)
  },
  { path: 'events', component: EventsComponent },
  { path: 'add-event', component: AddEventComponent },

  { path: 'evenements/:id', component: EventDetailComponent },
  {  path: 'conn',
    loadComponent: () => import('./conn/conn.component').then(m => m.ConnComponent)
  },
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'signup', component: AuthComponent },
  { path: 'adherent', component: AdherentComponent },
  { path: 'auth', component: AuthComponent },
  { path: 'recettes', component: RecipesComponent },
  { path: 'recette/:id', component: RecipeDetailsComponent },
  { path: 'add-recipe', component: AddRecipeComponent }
];
