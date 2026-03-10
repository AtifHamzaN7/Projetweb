import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { AdherentService } from './adherent.service';

describe('AdherentService', () => {
  let service: AdherentService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AdherentService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
