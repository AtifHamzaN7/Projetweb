import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Adherent {
  idAdh: number;
  nom: string;
  prenom: string;
  email: string;
  password: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdherentService {
  private baseUrl = 'http://localhost:8080/adherents';

  constructor(private http: HttpClient) {}

  getByEmailAndPassword(email: string, password: string): Observable<Adherent> {
    return this.http.get<Adherent>(`${this.baseUrl}/connexion`, {
      params: { email, password }
    });
  }

  // Tu peux ajouter d'autres méthodes ici si nécessaire
}
