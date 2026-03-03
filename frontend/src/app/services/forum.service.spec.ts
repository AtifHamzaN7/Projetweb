import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { ForumService } from './forum.service';

describe('ForumService', () => {
  let service: ForumService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(ForumService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getAllDiscussions should GET /discussions', () => {
    service.getAllDiscussions().subscribe((discussions) => {
      expect(discussions.length).toBe(1);
      expect(discussions[0].titre).toBe('T');
    });

    const req = httpMock.expectOne((r) => r.method === 'GET' && r.url === 'http://localhost:8080/discussions');
    req.flush([
      {
        idDisc: 1,
        titre: 'T',
        question: 'Q',
        auteur: { idAdh: 1, nom: 'N', prenom: 'P', email: 'e' }
      }
    ]);
  });

  it('postDiscussion should POST /discussions/ajout with params', () => {
    service.postDiscussion('T', 'Q', 5).subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = httpMock.expectOne((r) =>
      r.method === 'POST' &&
      r.url === 'http://localhost:8080/discussions/ajout' &&
      r.params.get('titre') === 'T' &&
      r.params.get('question') === 'Q' &&
      r.params.get('auteurId') === '5'
    );
    expect(req.request.body).toBeNull();
    req.flush(null);
  });

  it('getMessagesByDiscussion should GET /discussions/{id}/messages', () => {
    service.getMessagesByDiscussion(9).subscribe((messages) => {
      expect(messages.length).toBe(1);
      expect(messages[0].content).toBe('Hi');
    });

    const req = httpMock.expectOne((r) => r.method === 'GET' && r.url === 'http://localhost:8080/discussions/9/messages');
    req.flush([
      {
        idMsg: 1,
        content: 'Hi',
        auteur: { idAdh: 1, nom: 'N', prenom: 'P', email: 'e' },
        discussion: { idDisc: 9, titre: 'T', question: 'Q', auteur: { idAdh: 1, nom: 'N', prenom: 'P', email: 'e' } }
      }
    ]);
  });

  it('postMessage should POST /discussions/{id}/messages/ajout with params', () => {
    service.postMessage(3, 4, 'Hello').subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = httpMock.expectOne((r) =>
      r.method === 'POST' &&
      r.url === 'http://localhost:8080/discussions/3/messages/ajout' &&
      r.params.get('auteurId') === '4' &&
      r.params.get('content') === 'Hello'
    );
    expect(req.request.body).toBeNull();
    req.flush(null);
  });

  it('getMessagesByAdherent should GET /adherents/{id}/messages', () => {
    service.getMessagesByAdherent(2).subscribe((messages) => {
      expect(messages.length).toBe(0);
    });

    const req = httpMock.expectOne((r) => r.method === 'GET' && r.url === 'http://localhost:8080/adherents/2/messages');
    req.flush([]);
  });
});

