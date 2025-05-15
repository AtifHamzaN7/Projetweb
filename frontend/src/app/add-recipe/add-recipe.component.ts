import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RecetteService, Ingredient } from '../services/recipe.service';

@Component({
  selector: 'app-add-recipe',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './add-recipe.component.html',
  styleUrls: ['./add-recipe.component.css']
})
export class AddRecipeComponent {
  recipe = {
    nom: '',
    ingredients: [] as Ingredient[],
    etapes: [''],
    photo: '',
    auteur: null,
    categoriesString: '',
    categories: [] as string[]
  };

  selectedFile: File | null = null;

  constructor(private recetteService: RecetteService, private router: Router) {}

  addIngredient(): void {
    this.recipe.ingredients.push({ nom: '', quantite: '' });
  }

  addStep(): void {
    this.recipe.etapes.push('');
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.recipe.photo = this.selectedFile.name;
    }
  }

  submitForm(): void {
    this.recipe.categories = this.recipe.categoriesString
      .split(',')
      .map(c => c.trim())
      .filter(c => c !== '');

    const formData = new FormData();
    formData.append('recette', new Blob([JSON.stringify(this.recipe)], { type: 'application/json' }));

    if (this.selectedFile) {
      formData.append('image', this.selectedFile);
    }

    this.recetteService.addRecipe(formData).subscribe({
      next: () => {
        alert('Recipe added successfully!');
        this.router.navigate(['/']);
      },
      error: err => {
        console.error('Error adding recipe:', err);
        alert('Failed to add recipe.');
      }
    });
  }
}
