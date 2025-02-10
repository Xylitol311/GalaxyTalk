FROM node:22-alpine3.19
RUN npm install -g serve
RUN mkdir /dist
ADD ./dist /dist
EXPOSE 3000
ENTRYPOINT ["serve", "-s", "dist"]
