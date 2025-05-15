import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { RecetteService, Recette } from '../services/recipe.service';

@Component({
  selector: 'app-recipes',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule
  ],
  templateUrl: './recipes.component.html',
  styleUrls: ['./recipes.component.css']
})
export class RecipesComponent implements OnInit {
  recettes: Recette[] = [];

  constructor(private recetteService: RecetteService) {}

  ngOnInit(): void {
    this.chargerRecettes();
  }

  private chargerRecettes(): void {
    this.recetteService.getRecettes().subscribe({
      next: (data: Recette[]) => {
        this.recettes = data;
        console.log(this.recettes); // pour debug
      },
      error: (err) => {
        console.error('Erreur lors du chargement des recettes :', err);
      }
    });
  }
}
