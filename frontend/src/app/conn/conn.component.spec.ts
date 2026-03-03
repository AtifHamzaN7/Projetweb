import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { ConnComponent } from './conn.component';

describe('ConnComponent', () => {
  let component: ConnComponent;
  let fixture: ComponentFixture<ConnComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConnComponent, HttpClientTestingModule],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConnComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
