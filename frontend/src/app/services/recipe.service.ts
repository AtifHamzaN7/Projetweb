import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Ingredient {
  nom: string;
  quantite: string;
}

export interface Auteur {
  idAdh: number;
  nom: string;
  prenom: string;
  email: string;
  password: string;
}

export interface Recette {
  idRec: number;
  nom: string;
  ingredients: Ingredient[];
  etapes: string[];
  photo: string;     // URL de l'image
  auteur: Auteur;
  categories:string[];
}

@Injectable({
  providedIn: 'root'
})
export class RecetteService {
  private apiUrl = 'http://localhost:8080/recettes'; // Waiting for the backend

  constructor(private http: HttpClient) {}

  getRecettes(): Observable<Recette[]> {
    return this.http.get<Recette[]>(this.apiUrl);
  }

  addRecipe(recipeData: FormData): Observable<any> {
    return this.http.post(`${this.apiUrl}/ajout`, recipeData);
  }
}
