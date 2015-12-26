/*
 * Copyright (C) 2015 Hannes Dorfmann
 * Copyright (C) 2015 Tickaroo, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.tickaroo.tikxml.reading;

import com.tickaroo.tikxml.XmlReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Hannes Dorfmann
 */
public class CatalogueReader implements XmlParser<Catalogue> {

  BooksReader booksReader = new BooksReader();

  @Override
  public Catalogue read(XmlReader reader) throws IOException {

    reader.beginElement();
    if (!reader.nextElementName().equals("catalog"))
      throw new IOException("<catalogue> expected at Path " + reader.getPath());

    Catalogue catalogue = new Catalogue();
    catalogue.books = new ArrayList<>();

    while (reader.hasElement()){
      reader.beginElement();
      if (reader.nextElementName().equals("book")){
        catalogue.books.add(booksReader.read(reader));
      }
      reader.endElement();
    }

    reader.endElement();

    return catalogue;
  }
}
