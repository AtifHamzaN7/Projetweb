import { Auteur } from './recipe.service';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Adherent } from './adherent.service';

export interface Event {
  id: number;
  titre: string;
  description: string;
  date: string;
  lieu: string;
  auteur: {
    nom: string;
    prenom: string;
    email: string;
  };
  participants: Adherent[];
}

@Injectable({ providedIn: 'root' })
export class EventService {
  private apiUrl = 'http://localhost:8080/evenements';

  constructor(private http: HttpClient) {}

  getEvenements(): Observable<Event[]> {
    return this.http.get<Event[]>(`http://localhost:8080/evenements`);
  }

  getEventById(id: number): Observable<Event> {
    return this.http.get<Event>(`${this.apiUrl}/${id}`);
  }

 createEvent(event: Partial<Event>, auteurId: number): Observable<Event> {
  return this.http.post<Event>('http://localhost:8080/evenements/ajout', null, {
    params: {
      titre: event.titre!,
      date: event.date!, // doit être une string formatée 'yyyy-MM-dd'
      lieu: event.lieu!,
      description: event.description!,
      auteurId: auteurId.toString()
    }
  });
}
participerAEvent(eventId: number, adherentId: number): Observable<void> {
  return this.http.post<void>(
    `http://localhost:8080/evenements/${eventId}/participer`,
    null,
    { params: { adherentId: adherentId.toString() } }
  );
}
getEvenementsParAdherent(idAdh: number): Observable<Event[]> {
  return this.http.get<Event[]>(`http://localhost:8080/adherents/${idAdh}/evenements`);
}
getEvenementsParParticipant(adherentId: number): Observable<Event[]> {
  return this.http.get<Event[]>(`http://localhost:8080/adherents/${adherentId}/evenements`);
}
}
