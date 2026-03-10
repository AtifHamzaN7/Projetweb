import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { RecetteService } from './recipe.service';

describe('RecetteService', () => {
  let service: RecetteService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(RecetteService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
