import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AdherentService } from './adherent.service';

describe('AdherentService', () => {
  let service: AdherentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(AdherentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getByEmailAndPassword should call /adherents/connexion with params', () => {
    service.getByEmailAndPassword('e@x.com', 'pw').subscribe((adherent) => {
      expect(adherent.idAdh).toBe(1);
    });

    const req = httpMock.expectOne((r) =>
      r.method === 'GET' &&
      r.url === 'http://localhost:8080/adherents/connexion' &&
      r.params.get('email') === 'e@x.com' &&
      r.params.get('password') === 'pw'
    );
    req.flush({ idAdh: 1, nom: 'N', prenom: 'P', email: 'e@x.com', password: 'pw' });
  });
});
