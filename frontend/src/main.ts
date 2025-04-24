import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import { SharedModule } from './app/shared/shared.module';

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));

  