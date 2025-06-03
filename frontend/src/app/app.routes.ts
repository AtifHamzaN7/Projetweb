import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { AdherentComponent } from './adherent/adherent.component';
import { RecipesComponent } from './recipes/recipes.component';
import { RecipeDetailsComponent } from './recipe-details/recipe-details.component';
import { AddRecipeComponent } from './add-recipe/add-recipe.component';
import { EventsComponent } from './events/events.component';
import { AddEventComponent } from './add-event/add-event.component';
import { EventDetailComponent } from './event-detail/event-detail.component';
import { AuthComponent } from './auth/auth.component';



export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },
  { path: 'auth', loadComponent: () => import('./auth/auth.component').then(m => m.AuthComponent) },
  { path: 'conn', loadComponent: () => import('./conn/conn.component').then(m => m.ConnComponent) },
  { path: 'adherent', component: AdherentComponent },
  { path: 'recettes', component: RecipesComponent },
  { path: 'recette/:id', component: RecipeDetailsComponent },
  { path: 'add-recipe', component: AddRecipeComponent },
  { path: 'events', component: EventsComponent },
  { path: 'add-event', component: AddEventComponent },
  { path: 'evenements', component: EventsComponent },
    { path: 'evenements/ajouter', loadComponent: () => import('./add-event/add-event.component').then(m => m.AddEventComponent)},
  { path: 'evenements/:id', component: EventDetailComponent }
  
];
