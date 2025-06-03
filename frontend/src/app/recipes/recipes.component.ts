import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { RecetteService, Recette } from '../services/recipe.service';

@Component({
  selector: 'app-recipes',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule
  ],
  templateUrl: './recipes.component.html',
  styleUrls: ['./recipes.component.css']
})
export class RecipesComponent implements OnInit {
  recettes: Recette[] = [];
  filteredRecettes: Recette[] = [];
  searchTerm: string = '';
  showSuggestions: boolean = false;

  constructor(private recetteService: RecetteService) {}

  ngOnInit(): void {
    this.chargerRecettes();
  }

  private chargerRecettes(): void {
    this.recetteService.getRecettes().subscribe({
      next: (data: Recette[]) => {
        this.recettes = data;
        this.filteredRecettes = data;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des recettes :', err);
      }
    });
  }

  onSearch(event: Event): void {
    const searchValue = (event.target as HTMLInputElement).value.toLowerCase();
    this.searchTerm = searchValue;
    this.filterRecipes();
    this.showSuggestions = searchValue.length > 0;
  }

  filterRecipes(): void {
    if (!this.searchTerm) {
      this.filteredRecettes = this.recettes;
      return;
    }

    this.filteredRecettes = this.recettes.filter(recette => 
      recette.nom.toLowerCase().includes(this.searchTerm) ||
      recette.categories.some(cat => cat.toLowerCase().includes(this.searchTerm)) ||
      recette.auteur.nom.toLowerCase().includes(this.searchTerm) ||
      recette.auteur.prenom.toLowerCase().includes(this.searchTerm)
    );
  }

  selectSuggestion(recette: Recette): void {
    this.searchTerm = recette.nom;
    this.showSuggestions = false;
  }

  onFocusSearch(): void {
    if (this.searchTerm) {
      this.showSuggestions = true;
    }
  }

  onBlurSearch(): void {
    // Delayed hide to allow click events on suggestions
    setTimeout(() => {
      this.showSuggestions = false;
    }, 200);
  }
}