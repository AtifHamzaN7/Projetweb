import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { AuthComponent } from './auth/auth.component';
import { ConnComponent } from './conn/conn.component';
import { AdherentComponent } from './adherent/adherent.component';
import { RecipesComponent } from './recipes/recipes.component';
import { RecipeDetailsComponent } from './recipe-details/recipe-details.component';
import { AddRecipeComponent } from './add-recipe/add-recipe.component';

export const routes: Routes = [
  { path: 'home', component: HomeComponent },
  {
    path: 'auth',
    loadComponent: () => import('./auth/auth.component').then(m => m.AuthComponent)
  },
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
