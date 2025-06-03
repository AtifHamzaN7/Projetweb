import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Adherent {
  idAdh: number;
  nom: string;
  prenom: string;
  email: string;
}

export interface Discussion {
  idDisc: number;
  titre: string;
  question: string;
  auteur: Adherent;
}

export interface Message {
  idMsg: number;
  content: string;
  auteur: Adherent;
  discussion: Discussion;
}

@Injectable({
  providedIn: 'root'
})
export class ForumService {
  private apiUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  // 🔹 Obtenir toutes les discussions
  getAllDiscussions(): Observable<Discussion[]> {
    return this.http.get<Discussion[]>(`${this.apiUrl}/discussions`);
  }

  // 🔹 Ajouter une discussion
  postDiscussion(titre: string, question: string, auteurId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/discussions/ajout`, null, {
      params: {
        titre,
        question,
        auteurId: auteurId.toString()
      }
    });
  }

  // 🔹 Obtenir tous les messages d’une discussion
  getMessagesByDiscussion(id: number): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.apiUrl}/discussions/${id}/messages`);
  }

  // 🔹 Poster un message dans une discussion
  postMessage(discussionId: number, adherentId: number, content: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/messages/ajout`, null, {
      params: {
        content,
        discussionId: discussionId.toString(),
        auteurId: adherentId.toString()
      }
    });
  }

  // 🔹 Obtenir tous les messages d’un adhérent
  getMessagesByAdherent(id: number): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.apiUrl}/adherents/${id}/messages`);
  }
}
