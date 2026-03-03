import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { EventService } from './event.service';

describe('EventService', () => {
  let service: EventService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(EventService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getEvenements should GET /evenements', () => {
    service.getEvenements().subscribe((events) => {
      expect(events.length).toBe(1);
      expect(events[0].titre).toBe('Meet');
    });

    const req = httpMock.expectOne((r) => r.method === 'GET' && r.url === 'http://localhost:8080/evenements');
    req.flush([
      {
        id: 1,
        titre: 'Meet',
        description: 'D',
        date: '2026-03-03',
        lieu: 'Toulouse',
        auteur: { idAdh: 1, nom: 'N', prenom: 'P', email: 'e', password: 'pw' },
        participants: []
      }
    ]);
  });

  it('getEventById should GET /evenements/{id}', () => {
    service.getEventById(7).subscribe((event) => {
      expect(event.id).toBe(7);
    });

    const req = httpMock.expectOne((r) => r.method === 'GET' && r.url === 'http://localhost:8080/evenements/7');
    req.flush({
      id: 7,
      titre: 'T',
      description: 'D',
      date: '2026-03-03',
      lieu: 'L',
      auteur: { idAdh: 1, nom: 'N', prenom: 'P', email: 'e', password: 'pw' },
      participants: []
    });
  });

  it('createEvent should POST /evenements/ajout with query params', () => {
    service
      .createEvent({ titre: 'T', date: '2026-03-03', lieu: 'L', description: 'D' }, 9)
      .subscribe((event) => {
        expect(event.titre).toBe('T');
      });

    const req = httpMock.expectOne((r) =>
      r.method === 'POST' &&
      r.url === 'http://localhost:8080/evenements/ajout' &&
      r.params.get('titre') === 'T' &&
      r.params.get('date') === '2026-03-03' &&
      r.params.get('lieu') === 'L' &&
      r.params.get('description') === 'D' &&
      r.params.get('auteurId') === '9'
    );
    expect(req.request.body).toBeNull();
    req.flush({
      id: 1,
      titre: 'T',
      description: 'D',
      date: '2026-03-03',
      lieu: 'L',
      auteur: { idAdh: 9, nom: 'N', prenom: 'P', email: 'e', password: 'pw' },
      participants: []
    });
  });

  it('participerAEvent should POST /evenements/{id}/participer with adherentId param', () => {
    service.participerAEvent(3, 4).subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = httpMock.expectOne((r) =>
      r.method === 'POST' &&
      r.url === 'http://localhost:8080/evenements/3/participer' &&
      r.params.get('adherentId') === '4'
    );
    expect(req.request.body).toBeNull();
    req.flush(null);
  });

  it('getEvenementsParAdherent should GET /adherents/{id}/evenements', () => {
    service.getEvenementsParAdherent(2).subscribe((events) => {
      expect(events.length).toBe(0);
    });

    const req = httpMock.expectOne((r) => r.method === 'GET' && r.url === 'http://localhost:8080/adherents/2/evenements');
    req.flush([]);
  });

  it('getEvenementsParParticipant should GET /adherents/{id}/participations', () => {
    service.getEvenementsParParticipant(2).subscribe((events) => {
      expect(events.length).toBe(0);
    });

    const req = httpMock.expectOne((r) => r.method === 'GET' && r.url === 'http://localhost:8080/adherents/2/participations');
    req.flush([]);
  });
});
