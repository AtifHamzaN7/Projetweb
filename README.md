# Projetweb

Before runing the frontend 
you should install NodeJs

and run the following commands

npm install 
npm install -g @angular/cli


To run the frontend 

cd frontend
ng serve




FrontEnd Structure

src/app/
├── auth/         <->
├── home/         <->   
├── user/         <->   imane
├── recipes/      <->   oussama
├── events/
├── forum/ 
├── admin/         *
├── shared/ 
├── services/
├── models/

in 
services/
├── auth.service.ts           # login, register, token handling
├── recipe.service.ts         # CRUD for recipes
├── event.service.ts          # CRUD for events
├── forum.service.ts          # fetch discussions, post messages
├── user.service.ts           # user profile, ingredients, cotisation
├── admin.service.ts          # admin-only actions


in 
models/
├── user.model.ts
├── recipe.model.ts
├── event.model.ts
├── ingredient.model.ts
├── discussion.model.ts
├── message.model.ts


in 
recipes/
├── recipe-list/
│   ├── recipe-list.component.ts
│   ├── recipe-list.component.html
│   ├── recipe-list.component.css
├── add-recipe/
│   ├── add-recipe.component.ts
│   ├── add-recipe.component.html
│   ├── add-recipe.component.css
├── edit-recipe/
│   ├── edit-recipe.component.ts
│   ├── edit-recipe.component.html
│   ├── edit-recipe.component.css
├── recipe-detail/
│   ├── recipe-detail.component.ts
│   ├── recipe-detail.component.html
│   ├── recipe-detail.component.css


in 
├── shared/           # ✅ reusable UI like navbar, footer, buttons
│   ├── navbar/
│   ├── footer/
│   ├── card/
│   ├── page-header/