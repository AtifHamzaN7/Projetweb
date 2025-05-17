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
      this.selectedFile = input.files[0];
      this.recipe.photo = this.selectedFile.name;
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
    this.recipe.categories = this.recipe.categoriesString
      .split(',')
      .map(c => c.trim())
      .filter(c => c !== '');

    const formData = new FormData();
    formData.append('nom', this.recipe.nom);
    formData.append('photo', this.recipe.photo);
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

    if (this.selectedFile) {
      formData.append('image', this.selectedFile);
    }

    // ✅ Ajoute ce bloc juste ici :
    console.log('FormData contents:');
    formData.forEach((value, key) => {
      console.log(`${key}:`, value);
    });

    // ✅ Puis envoie la requête :
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
