import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { RecetteService } from './recipe.service';

describe('RecetteService', () => {
  let service: RecetteService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(RecetteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getRecettes should GET /recettes', () => {
    service.getRecettes().subscribe((recettes) => {
      expect(recettes.length).toBe(1);
      expect(recettes[0].nom).toBe('Cake');
    });

    const req = httpMock.expectOne((r) => r.method === 'GET' && r.url === 'http://localhost:8080/recettes');
    req.flush([
      {
        idRec: 1,
        nom: 'Cake',
        ingredients: [{ nom: 'sucre', quantite: '10g' }],
        etapes: ['step1'],
        photo: 'img.jpg',
        auteur: { idAdh: 1, nom: 'A', prenom: 'B', email: 'e', password: 'p' },
        categories: ['vegan']
      }
    ]);
  });

  it('addRecipe should POST /recettes/ajout with FormData', () => {
    const fd = new FormData();
    fd.append('nom', 'Cake');

    service.addRecipe(fd).subscribe((res) => {
      expect(res).toEqual({ ok: true });
    });

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === 'http://localhost:8080/recettes/ajout');
    expect(req.request.body instanceof FormData).toBeTrue();
    req.flush({ ok: true });
  });
});
