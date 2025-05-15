import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { RecetteService, Recette } from '../services/recipe.service';

@Component({
  selector: 'app-recipe-details',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './recipe-details.component.html',
  styleUrls: ['./recipe-details.component.css']
})
export class RecipeDetailsComponent implements OnInit {
  recette: Recette | null = null;

  constructor(
    private route: ActivatedRoute,
    private recetteService: RecetteService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.recetteService.getRecettes().subscribe((recettes) => {
      this.recette = recettes.find(r => r.idRec === id) || null;
    });
  }
}
