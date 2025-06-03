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
    auteur: null as number | null,
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
    const file = input.files[0];
    // Store the filename immediately
    this.recipe.photo = file.name;
    
    const formData = new FormData();
    formData.append('file', file);
    
    this.recetteService.uploadImage(formData).subscribe({
      next: (response: string) => {
        console.log('File uploaded successfully');
      },
      error: (error) => {
        console.error('Error uploading file:', error);
        // Reset photo name if upload fails
        this.recipe.photo = '';
      }
    });
  }
}


ngOnInit(): void {
  const stored = localStorage.getItem('adherent');
  if (!stored) {
    this.router.navigate(['/sign-in']);
    return;
  }

  const adherent = JSON.parse(stored);
  console.log('Adhérent connecté :', adherent); // ✅ Ajoute ce log

  this.recipe.auteur = adherent.idAdh; // Assure-toi que c'est bien 'id' et pas 'adherentId'
}



submitForm(): void {
  console.log('Submitting recipe with photo:', this.recipe.photo); // Debug log

  // Prepare categories
  this.recipe.categories = this.recipe.categoriesString
    .split(',')
    .map(c => c.trim())
    .filter(c => c !== '');

  const formData = new FormData();
  formData.append('nom', this.recipe.nom);
  formData.append('photo', this.recipe.photo); // Make sure photo is included
  formData.append('auteurId', (this.recipe.auteur ?? '').toString());

  this.recipe.etapes.forEach(etape => {
    formData.append('etapes', etape);
  });

  this.recipe.categories.forEach(cat => {
    formData.append('categories', cat);
  });

  const ingredientsStr = this.recipe.ingredients
    .map(i => `(${i.nom},0,${i.quantite})`)
    .join(';');
  formData.append('ingredients', ingredientsStr);

  this.recetteService.addRecipe(formData).subscribe({
    next: () => {
      alert('Recipe added successfully!');
      this.router.navigate(['/recettes']);
    },
    error: err => {
      console.error('Error adding recipe:', err);
      alert('Failed to add recipe.');
    }
  });
}

  

}
