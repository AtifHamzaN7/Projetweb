import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConnComponent } from './conn.component';

describe('ConnComponent', () => {
  let component: ConnComponent;
  let fixture: ComponentFixture<ConnComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConnComponent]
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
