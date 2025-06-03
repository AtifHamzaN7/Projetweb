import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Adherent } from './adherent.service';

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

@Injectable({ providedIn: 'root' })
export class ForumService {
  private api = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  getAllDiscussions(): Observable<Discussion[]> {
    return this.http.get<Discussion[]>(`${this.api}/discussions`);
  }

  getMessagesByDiscussion(id: number): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.api}/discussions/${id}/messages`);
  }

  postMessage(discussionId: number, adherentId: number, content: string): Observable<void> {
    return this.http.post<void>(`${this.api}/messages/ajout`, null, {
      params: { discussionId, auteurId: adherentId, content }
    });
  }

  getMessagesByAdherent(id: number): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.api}/adherents/${id}/messages`);
  }

  postDiscussion(titre: string, question: string, auteurId: number): Observable<void> {
    return this.http.post<void>(`${this.api}/discussions/ajout`, null, {
      params: { titre, question, auteurId }
    });
  }
}
