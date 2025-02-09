FROM node:22.13.1-alpine
RUN npm install -g serve
RUN mkdir /dist
ADD ./dist /dist
EXPOSE 3000
ENTRYPOINT ["serve", "-s", "dist"]
