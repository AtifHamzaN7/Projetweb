import { Routes } from '@angular/router';
import { HomeComponent } from './home/home/home.component';
import { AuthComponent } from './auth/auth.component';
import { RecipesComponent } from './recipes/recipes.component';
import { RecipeDetailsComponent } from './recipe-details/recipe-details.component';

export const routes: Routes = [
  { path: 'home', component: HomeComponent },
  { path: 'auth', component: AuthComponent },
  { path: 'recettes', component: RecipesComponent },
  { path: 'recette/:id', component: RecipeDetailsComponent }
];


